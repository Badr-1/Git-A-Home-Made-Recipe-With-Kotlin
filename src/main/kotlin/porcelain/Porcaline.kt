package porcelain

import plumbing.*
import utils.*
import java.io.File
import java.nio.file.Files
import java.time.*
import java.util.*
import kotlin.io.path.Path

object Config {
    /**
     * consists of the following:
     * - sections, eg [ core ]
     * - keys, eg repositoryformatversion
     * - values, eg 0
     * */
    private fun getConfigFile() = File("${System.getProperty("user.dir")}/.kit/config")
    private val sections = mutableListOf("core") // to be updated when more sections are added
    private val values = mutableMapOf(
        "core" to mutableMapOf(
            "repositoryformatversion" to "0", "filemode" to "true", "bare" to "false", "logallrefupdates" to "true"
        )
    )

    fun unset() {
        sections.removeIf { it != "core" }
    }

    fun write() {
        getConfigFile().createNewFile()
        getConfigFile().writeText("")
        for (section in sections) {
            getConfigFile().appendText("[$section]\n")
            for ((key, value) in values[section]!!) {
                getConfigFile().appendText("\t$key = $value\n")
            }
        }
    }

    fun set(sectionWithKey: String, value: String) {
        val section = sectionWithKey.split(".")[0]
        val key = sectionWithKey.split(".")[1]
        if (!sections.contains(section)) {
            sections.add(section)
            values[section] = mutableMapOf()
        }
        values[section]!![key] = value
        write()
    }

    fun get(sectionWithKey: String): String {
        val section = sectionWithKey.split(".")[0]
        val key = sectionWithKey.split(".")[1]
        if (section == "user" && (key == "name" || key == "email")) {
            if (!sections.contains(section)) {
                sections.add(section)
                values[section] = mutableMapOf()
            }
            if (values[section]!![key] == null) values[section]!![key] = "Kit $key"
        }
        return values[section]!![key]!!
    }
}


fun init(repositoryName: String = ""): String {
    var path = "${System.getProperty("user.dir")}/"
    if (repositoryName.isNotEmpty()) {
        File("$path/$repositoryName").mkdir()
        if (!path.contains(repositoryName)) {
            path += "$repositoryName/"
        }
        System.setProperty("user.dir", path)
        if (File("$path.kit").exists()) {
            return "Reinitialized existing Kit repository in ${File("${path}.kit").absolutePath}"
        }
    }
    // repository
    File("${path}.kit").mkdir()
    // objects database
    File("${path}.kit/objects").mkdir()
    // refs
    File("${path}.kit/refs").mkdir()
    File("${path}.kit/refs/heads").mkdir()
    // HEAD
    File("${path}.kit/HEAD").writeText("ref: refs/heads/master")
    // config
    // default config
    Config.unset()
    Config.write()
    return "Initialized empty Kit repository in ${File("${path}.kit").absolutePath}"
}

fun add(filePath: String) {
    // check if the file exists
    if (!File(filePath).exists()) {
        throw Exception("fatal: pathspec '$filePath' did not match any files")
    }
    // check if the file is in the working directory
    if (!File(filePath).absolutePath.startsWith(File(System.getProperty("user.dir")).absolutePath)) {
        throw Exception("fatal: pathspec '$filePath' is outside repository")
    }
    // check if the file is in the .kit directory
    if (File(filePath).absolutePath.startsWith(File("${System.getProperty("user.dir")}/.kit").absolutePath)) {
        return
    }
    val file = File(filePath)
    // update the index
    val mode = getMode(file)
    updateIndex(filePath, "-a", hashObject(filePath, write = true), mode)
}

fun getMode(file: File): String {
    val mode = when {
        // check if the file is executable
        file.canExecute() -> "100755"
        // check if the file is a symlink
        Files.isSymbolicLink(Path(file.path)) -> "120000"
        // then it's a normal file
        else -> "100644"
    }
    return mode
}

fun unstage(filePath: String) {
    // check if the file is in the working directory
    if (!File(filePath).absolutePath.startsWith(File(System.getProperty("user.dir")).absolutePath)) {
        throw Exception("fatal: pathspec '$filePath' is outside repository")
    }
    // check if the file exists or is in the index
    if (GitIndex.get(File(filePath).relativePath()) == null) {
        throw Exception("fatal: pathspec '$filePath' did not match any files")
    }
    updateIndex(filePath, "-d")
}

fun status(): String {
    // get all the files in the working directory except the .kit directory
    val workingDirectoryFiles =
        File(System.getProperty("user.dir")).walk().filter { it.isFile && !it.path.contains(".kit") }.toList()
            .map { it.relativePath() }

    // get all the files in the index
    val indexFiles = GitIndex.entries().map { it.path }

    // get all files in the HEAD commit tree
    val headCommitTreeFiles = getHeadCommitTreeFiles()

    val untrackedFiles = mutableListOf<String>()
    val unStagedChanges = mutableListOf<String>()
    val stagedChanges = mutableListOf<String>()

    // untracked files are files that are in the working directory but not in the index
    untrackedFiles.addAll(workingDirectoryFiles.filter { !indexFiles.contains(it) })

    /**
     * unStaged changes are the following (index vs working directory):
     * 1. content: the sha1 is different => modified
     * 2. mode: the mode is different => modified
     * 3. deleted: the file is deleted => deleted
     */
    // add modified files (content)
    unStagedChanges.addAll(
        workingDirectoryFiles.filter { indexFiles.contains(it) }
            .filter { GitIndex.get(it)!!.sha1 != hashObject("${System.getProperty("user.dir")}/$it") }
            .map { "M $it" }
    )
    // add modified files (mode)
    unStagedChanges.addAll(
        workingDirectoryFiles.filter { indexFiles.contains(it) }
            .filter { GitIndex.get(it)!!.mode != getMode(File("${System.getProperty("user.dir")}/$it")).toInt(8) }
            .map { "M $it" }
    )
    // add deleted files
    unStagedChanges.addAll(
        indexFiles.filter { !workingDirectoryFiles.contains(it) }
            .map { "D $it" }
    )
    /**
     * staged changes are the following (index vs HEAD commit tree):
     * 1. content: the sha1 is different => modified
     * 2. mode: the mode is different => modified
     * 3. deleted: the file is deleted => deleted
     * 4. added: the file is added => added
     */
    if (headCommitTreeFiles.isEmpty()) {
        // add added files
        stagedChanges.addAll(
            workingDirectoryFiles.filter { indexFiles.contains(it) }
                .map { "A $it" }
        )
    } else {
        // add added files
        stagedChanges.addAll(
            indexFiles.filter { !headCommitTreeFiles.map { headEntry -> headEntry.path }.contains(it) }
                .map { "A $it" }
        )
        // add modified files (content)
        stagedChanges.addAll(
            indexFiles.filter { headCommitTreeFiles.map { headEntry -> headEntry.path }.contains(it) }
                .filter {common -> GitIndex.get(common)!!.sha1 != headCommitTreeFiles.find { it.path == common }!!.hash }
                .map { "M $it" }
        )
        // add modified files (mode)
        stagedChanges.addAll(
            indexFiles.filter { headCommitTreeFiles.map { headEntry -> headEntry.path }.contains(it) }
                .filter {common -> GitIndex.get(common)!!.mode != headCommitTreeFiles.find { it.path == common }!!.mode.toInt(8) }
                .map { "M $it" }
        )
        // add deleted files
        stagedChanges.addAll(
            headCommitTreeFiles.map { it.path }.filter { !indexFiles.contains(it) }
                .map { "D $it" }
        )
    }


    return statusString(untrackedFiles, stagedChanges, unStagedChanges)
}

fun statusString(
    untrackedFiles: List<String>,
    staged: List<String>,
    unStaged: List<String>,
): String {
    return """
        On branch master
        
        Untracked files:
        ${untrackedFiles.sorted().joinToString("\n\t\t") { "?? $it" }}
        
        Changes to be committed :
        ${staged.sorted().joinToString("\n\t\t") { it }}
        
        Changes not staged for commit:
        ${unStaged.sorted().joinToString("\n\t\t") { it }}

        """
}

fun getHeadCommitTreeFiles(): MutableList<TreeEntry> {
    val head = getHead()
    return if (!head.matches(Regex("[0-9a-f]{40}"))) {
        return try {
            val branch = getBranchCommit(head)
            getTreeEntries(getTreeHash(branch)).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    } else {
        getTreeEntries(getTreeHash(head)).toMutableList()
    }

}

// TODO think about adding amend option
fun commit(message: String): String {
    val head = getHead()
    val parent = when {
        head.matches(Regex("[0-9a-f]{40}")) -> head
        else -> {
            if (File("${System.getProperty("user.dir")}/.kit/refs/heads/$head").exists()) getBranchCommit(head)
            else ""
        }
    }
    val treeHash = writeTree(System.getProperty("user.dir"), write = true)
    val commitHash = commitTree(treeHash, message, parent)
    if (head.matches(Regex("[0-9a-f]{40}"))) {
        File("${System.getProperty("user.dir")}/.kit/HEAD").writeText(commitHash)
    } else {
        val branch = File("${System.getProperty("user.dir")}/.kit/refs/heads/$head")
        branch.createNewFile()
        branch.writeText(commitHash)
    }
    return commitHash
}

fun checkout(ref: String) {
    // ref could be a commit hash or a branch name
    val commitHash = when {
        // TODO think about supporting working with commit hashes less than 40 characters (short commit hashes)
        // TODO think about checking out files
        ref.matches(Regex("[0-9a-f]{40}")) -> ref
        else -> {
            val branch = File("${System.getProperty("user.dir")}/.kit/refs/heads/$ref")
            if (!branch.exists()) {
                throw Exception("error: pathspec '$ref' did not match any file(s) known to kit")
            }
            branch.readText()
        }
    }
    // change the HEAD
    File("${System.getProperty("user.dir")}/.kit/HEAD").writeText(commitHash)
    updateWorkingDirectory(commitHash)
}

// this is a helper function for the actual log command
fun getHistory(): List<String> {
    val commits = mutableListOf<String>()
    val head = getHead()
    var it = head
    if (!head.matches(Regex("[0-9a-f]{40}"))) { // if the head is a branch
        try {
            it = getBranchCommit(head) // throw an exception if the branch doesn't exist
        } catch (e: Exception) {
            return commits // return an empty list
        }
    }
    do {
        commits.add(it)
        it = getParent(it)
    } while (it != "")

    return commits
}

fun getRefs(): MutableMap<String, String> {
    val refs = mutableMapOf<String, String>()
    val branches = File("${System.getProperty("user.dir")}/.kit/refs/heads").walk().filter { it.isFile }.toList()
    for (branch in branches) {
        refs[branch.relativePath("${System.getProperty("user.dir")}/.kit/refs/heads")] = branch.readText()
    }
    return refs
}

fun log() {
    val commits = getHistory()
    if (commits.isEmpty())
        return
    val branches = getRefs()
    val head = when {
        File("${System.getProperty("user.dir")}/.kit/HEAD").readText().matches(Regex("[0-9a-f]{40}")) -> getHead()
        else -> {
            var head = File("${System.getProperty("user.dir")}/.kit/HEAD").readText().split(" ")[1]
            head =
                File("${System.getProperty("user.dir")}/.kit/$head").relativeTo(File("${System.getProperty("user.dir")}/.kit/refs/heads")).path
            head
        }
    }
    for (commit in commits) {
        val commitContent = catFile(commit, "-p")
        val hasParent = if (commitContent.contains("parent")) 0 else 1
        val authorLine = commitContent.split("\n")[2 - hasParent].split(" ").toMutableList()
        authorLine.removeFirst()
        authorLine.removeLast()
        val unixEpoch = authorLine.removeLast()
        val date = Date(unixEpoch.toLong() * 1000)
        val today = Date()
        val timeDifference = calculateDateTimeDifference(
            date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
            today.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        )
        val author = authorLine.joinToString(" ").green()
        val message = commitContent.split("\n")[4 - hasParent]
        val refs = branches.filter { it.value == commit }.map { it.key }.toMutableList()
        if (commit == head) {
            refs.add("HEAD")
        } else if (refs.contains(head)) {
            refs.remove(head)
            refs.add("HEAD -> $head")
        }
        println(
            "* ${
                commit.substring(
                    0, 7
                ).red()
            }${if (refs.isNotEmpty()) " (${refs.joinToString()})".yellow() else ""} $message <$author> (${timeDifference.green()})"
        )
    }
}

fun calculateDateTimeDifference(startDate: LocalDateTime, endDate: LocalDateTime): String {
    val duration = Duration.between(startDate, endDate)
    val period = Period.between(startDate.toLocalDate(), endDate.toLocalDate())

    val years = period.years
    val months = period.months
    val days = period.days

    val hours = duration.toHours() % 24
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60

    // return the most significant time unit
    return when {
        years > 0 -> "$years year${if (years > 1) "s" else ""} ago"
        months > 0 -> "$months month${if (months > 1) "s" else ""} ago"
        days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
        hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
        seconds > 0 -> "$seconds second${if (seconds > 1) "s" else ""} ago"
        else -> "just now"
    }
}

fun getParent(commitHash: String): String {
    val content = catFile(commitHash, "-p")
    return if (content.contains("parent")) {
        content.split("\n")[1].split(" ")[1]
    } else {
        ""
    }
}

fun getHead(): String {
    val head = File("${System.getProperty("user.dir")}/.kit/HEAD").readText()
    return if (head.contains("ref: refs/heads/")) {
        val branch =
            File(System.getProperty("user.dir") + "/.kit/" + head.split(" ")[1]).relativePath("${System.getProperty("user.dir")}/.kit/refs/heads")
        branch
    } else {
        head
    }
}

fun branch(branchName: String, ref: String = "HEAD") {
    val branch = File("${System.getProperty("user.dir")}/.kit/refs/heads/$branchName")
    if (branch.exists()) {
        throw Exception("fatal: A branch named '$branchName' already exists.")
    } else {
        if (branchName.contains("/")) {
            branch.parentFile.mkdirs()
        }
        branch.createNewFile()
    }
    when (ref) {
        "HEAD" -> {
            val head = getHead()
            val dest = when {
                head.matches(Regex("[0-9a-f]{40}")) -> head
                else -> {
                    getBranchCommit(head)
                }
            }
            branch.writeText(dest)
        }

        else -> {
            branch.writeText(ref)
        }
    }

}

private fun getBranchCommit(branch: String): String {
    if (!File("${System.getProperty("user.dir")}/.kit/refs/heads/$branch").exists()) {
        throw Exception("fatal: Not a valid object name: '$branch'.")
    }
    return File("${System.getProperty("user.dir")}/.kit/refs/heads/$branch").readText()
}

fun updateWorkingDirectory(commitHash: String) {
    // get the tree hash from the commit
    val treeHash = getTreeHash(commitHash)
    val treeEntries = getTreeEntries(treeHash)
    treeEntries.forEach {
        val file = File("${System.getProperty("user.dir")}/${it.path}")
        if (it.mode == "100755") {
            file.setExecutable(true)
        }
        file.createNewFile()
        file.writeText(catFile(it.hash, "-p"))
    }
}

fun getTreeHash(commitHash: String): String {
    val content = catFile(commitHash, "-p")
    return content.split("\n")[0].split(" ")[1]
}

fun getTreeEntries(treeHash: String): List<TreeEntry> {
    val content = catFile(treeHash, "-p")
    val treeEntries = mutableListOf<TreeEntry>()
    content.split("\n").map {
        val mode = it.split(" ")[0]
        val sha1 = it.split(" ")[2].split("\t")[0]
        val path = it.split("\t")[1]
        if (mode == "40000") {
            treeEntries.addAll(getTreeEntries(sha1).map { treeEntry ->
                TreeEntry(
                    treeEntry.mode, "$path/${treeEntry.path}", treeEntry.hash
                )
            })
        } else {
            treeEntries.add(TreeEntry(mode, path, sha1))
        }
    }
    return treeEntries
}


fun String.red() = "\u001B[31m$this\u001B[0m"
fun String.green() = "\u001B[32m$this\u001B[0m"
fun String.yellow() = "\u001B[33m$this\u001B[0m"