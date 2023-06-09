package kit.porcelain

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kit.plumbing.GitIndex
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString

class PorcelainKtTest {

    @BeforeEach
    @AfterEach
    fun cleanUp() {
        // clean up
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.deleteRecursively()
    }

    @Test
    fun `initialize a repository`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        assertEquals(
            "Initialized empty Kit repository in ${File("${workingDirectory}/.kit").absolutePath}",
            init(workingDirectory)
        )
        assert(File("$workingDirectory/.kit").exists())
        assert(File("$workingDirectory/.kit/objects").exists())
        assert(File("$workingDirectory/.kit/refs").exists())
        assert(File("$workingDirectory/.kit/refs/heads").exists())
        assert(File("$workingDirectory/.kit/HEAD").exists())
        assert(File("$workingDirectory/.kit/HEAD").readText() == "ref: refs/heads/master")
        assert(File("$workingDirectory/.kit/config").exists())
    }

    @Test
    fun `initialize a repository with name`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        val repositoryName = "demo"
        assertEquals(
            "Initialized empty Kit repository in ${File("${workingDirectory}/$repositoryName/.kit").absolutePath}",
            init(Path.of(workingDirectory.pathString, repositoryName).toAbsolutePath())
        )
        assert(File("$workingDirectory/$repositoryName/.kit").exists())
        assert(File("$workingDirectory/$repositoryName/.kit/objects").exists())
        assert(File("$workingDirectory/$repositoryName/.kit/refs").exists())
        assert(File("$workingDirectory/$repositoryName/.kit/refs/heads").exists())
        assert(File("$workingDirectory/$repositoryName/.kit/HEAD").exists())
        assert(File("$workingDirectory/$repositoryName/.kit/HEAD").readText() == "ref: refs/heads/master")
        assert(File("$workingDirectory/$repositoryName/.kit/config").exists())
    }

    @Test
    fun `initialize a repository with name in an existing repository`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        val repositoryName = "demo"
        assertEquals(
            "Initialized empty Kit repository in ${File("${workingDirectory}/$repositoryName/.kit").absolutePath}",
            init(Path.of(workingDirectory.pathString,repositoryName).toAbsolutePath())
        )
        assertEquals(
            "Reinitialized existing Kit repository in ${File("${workingDirectory}/$repositoryName/.kit").absolutePath}",
            init(Path.of(workingDirectory.pathString,repositoryName).toAbsolutePath())
        )
    }

    @Test
    fun `add non-existent file`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        val filePath = "src/test/resources/workingDirectory/non-existent-file"
        val exception = assertThrows<Exception> {
            add(filePath)
        }
        assertEquals("fatal: pathspec '$filePath' did not match any files", exception.message)
    }

    @Test
    fun `add a file outside the repo`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        val filePath = "${workingDirectory.parent}/test.txt"
        File(filePath).writeText("test text")
        val exception = assertThrows<Exception> {
            add(filePath)
        }
        assertEquals("fatal: pathspec '$filePath' is outside repository", exception.message)
        File(filePath).delete()
    }

    @Test
    fun `add file inside the kit directory`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        val filePath = "${workingDirectory.absolutePathString()}/.kit/test.txt"
        File(filePath).writeText("test text")

        assertEquals(0, GitIndex.getEntryCount())
    }

    @Test
    fun `add a file`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")

        assertEquals(0, GitIndex.getEntryCount())
        add(filePath)
        assertEquals(1, GitIndex.getEntryCount())
    }

    @Test
    fun `remove a file that's not in the index`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        val exception = assertThrows<Exception> {
            unstage(filePath)
        }
        assertEquals("fatal: pathspec '$filePath' did not match any files", exception.message)
    }

    @Test
    fun `remove a file that's outside the repo`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        val filePath = "${workingDirectory.parent}/test.txt"
        File(filePath).writeText("test text")
        val exception = assertThrows<Exception> {
            unstage(filePath)
        }
        assertEquals("fatal: pathspec '$filePath' is outside repository", exception.message)
        File(filePath).delete()
    }

    @Test
    fun `remove a file inside the kit directory`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        val filePath = "${workingDirectory.absolutePathString()}/.kit/test.txt"
        File(filePath).writeText("test text")
        val exception = assertThrows<Exception> {
            unstage(filePath)
        }
        assertEquals("fatal: pathspec '$filePath' did not match any files", exception.message)
        File(filePath).delete()
    }

    @Test
    fun `remove a file from index`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        assertEquals(1, GitIndex.getEntryCount())
        unstage(filePath)
        assertEquals(0, GitIndex.getEntryCount())
    }

    @Test
    fun `status without a commit`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // array of files
        val files = arrayOf(
            "test.txt",
            "test2.txt",
            "test3.txt",
            "test4.txt"
        ).map { File("${workingDirectory.absolutePathString()}/$it") }.onEach { it.writeText("test text") }
        add(files[0].path)
        File("$workingDirectory/test").mkdir()
        files[0].renameTo(File("$workingDirectory/test/${files[0].name}"))
        add(files[1].path)
        status()
    }

    @Test
    fun `status with a head on a branch`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // array of files
        val files = arrayOf(
            "test.txt",
            "test2.txt",
            "test3.txt",
            "test4.txt"
        ).map { File("${workingDirectory.absolutePathString()}/$it") }.onEach { it.writeText("test text") }
        add(files[0].path)
        add(files[1].path)
        add(files[2].path)
        commit("test commit")
        // modify one of the files
        files[0].setExecutable(true)
        // delete one of the files
        files[1].delete()
        add(files[3].path)
        add(files[0].path)
        unstage(files[1].path)
        status()
    }

    @Test
    fun `status with a head on a detached state`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // array of files
        val files = arrayOf(
            "test.txt",
            "test2.txt",
            "test3.txt",
            "test4.txt"
        ).map { File("${workingDirectory.absolutePathString()}/$it") }.onEach { it.writeText("test text") }
        add(files[0].path)
        add(files[1].path)
        add(files[2].path)
        val hash = commit("test commit")
        checkout(hash)
        // modify one of the files
        files[0].setExecutable(true)
        // delete one of the files
        files[1].delete()
        add(files[3].path)
        add(files[0].path)
        unstage(files[1].path)
        status()
    }

    @Test
    fun `first commit on a clean tree`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        File("${workingDirectory.absolutePathString()}/test.txt").writeText("test text")
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        val exception = assertThrows<Exception> {
            commit("test commit")
        }
        assertEquals("nothing to commit, working tree clean", exception.message)
    }

    @Test
    fun `second commit on a clean tree`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        commit("test commit")
        val exception = assertThrows<Exception> {
            commit("test commit")
        }
        assertEquals("nothing to commit, working tree clean", exception.message)
    }


    @Test
    fun `commit on master`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val commitHash = commit("test commit")
        // this should create a file in the .kit/refs/heads named master
        assertTrue(File("$workingDirectory/.kit/refs/heads/master").exists())
        assertEquals(commitHash, File("$workingDirectory/.kit/refs/heads/master").readText())
    }

    @Test
    fun `commit twice`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        commit("test commit")
        File(filePath).writeText("test text 2")
        add(filePath)
        val commitHash2 = commit("test commit 2")
        assertEquals(commitHash2, File("$workingDirectory/.kit/refs/heads/master").readText())
    }

    @Test
    fun `commit when HEAD is at Detached state`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val commitHash = commit("test commit")
        // checkout to the commit
        File("$workingDirectory/.kit/HEAD").writeText(commitHash)
        File(filePath).writeText("test text 2")
        add(filePath)
        val commitHash2 = commit("test commit 2")

        assertEquals(commitHash2, File("$workingDirectory/.kit/HEAD").readText())
    }

    @Test
    fun `checkout non-existent branch`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        commit("test commit")
        val exception = assertThrows<Exception> {
            checkout("test")
        }
        assertEquals("error: pathspec 'test' did not match any file(s) known to kit", exception.message)
    }

    @Test
    fun `checkout a commit`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val commitHash = commit("test commit")
        File(filePath).writeText("test text 2")
        add(filePath)
        commit("test commit 2")
        checkout(commitHash)
        assertEquals(commitHash, File("$workingDirectory/.kit/HEAD").readText())
        // the content of the file should be the same as the checkout commit
        assertEquals("test text", File(filePath).readText())
    }

    @Test
    fun `checkout a tag`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val commitHash = commit("test commit")
        tag("test", "test tag")
        File(filePath).writeText("test text 2")
        add(filePath)
        commit("test commit 2")
        checkout("test")
        assertEquals(commitHash, File("$workingDirectory/.kit/HEAD").readText())
        // the content of the file should be the same as the checkout commit
        assertEquals("test text", File(filePath).readText())
        log()
    }

    @Test
    fun `checkout a branch`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        commit("test commit")
        File(filePath).writeText("test text 2")
        add(filePath)
        val commitHash = commit("test commit 2")
        checkout("master")
        assertEquals(commitHash, File("$workingDirectory/.kit/refs/heads/master").readText())
    }

    @Test
    fun `checkout an executable file`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.sh"
        File(filePath).writeText("echo test")
        File(filePath).setExecutable(true)
        add(filePath)
        val commitHash = commit("test commit")
        File(filePath).setExecutable(false)
        add(filePath)
        commit("test commit 2")
        checkout(commitHash)
        assertEquals(commitHash, File("$workingDirectory/.kit/HEAD").readText())
        // the content of the file should be the same as the checkout commit
        assertEquals("echo test", File(filePath).readText())
        assertTrue(File(filePath).canExecute())
    }

    @Test
    fun `checkout a directory`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test/test.txt"
        File(filePath).parentFile.mkdirs()
        File(filePath).writeText("test text")
        add(filePath)
        val commitHash = commit("test commit")
        File(filePath).writeText("test text 2")
        add(filePath)
        commit("test commit 2")
        checkout(commitHash)
        assertEquals(commitHash, File("$workingDirectory/.kit/HEAD").readText())
        // the content of the file should be the same as the checkout commit
        assertEquals("test text", File(filePath).readText())
    }

    @Test
    fun `make an existent branch`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        commit("test commit")
        val exception = assertThrows<Exception> {
            branch("master")
        }
        assertEquals("fatal: A branch named 'master' already exists.", exception.message)
    }

    @Test
    fun `make a new branch without directories`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val hash = commit("test commit")
        branch("test")
        assertEquals(hash, File("$workingDirectory/.kit/refs/heads/test").readText())

    }

    @Test
    fun `make a new branch with directories`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val hash = commit("test commit")
        branch("feature/test")
        assertEquals(hash, File("$workingDirectory/.kit/refs/heads/feature/test").readText())

    }

    @Test
    fun `make a new branch with a ref`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val hash = commit("test commit")
        branch("test", hash)
        assertEquals(hash, File("$workingDirectory/.kit/refs/heads/test").readText())

    }

    @Test
    fun `make a new branch without a ref`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val hash = commit("test commit")
        checkout(hash)
        branch("test")
        assertEquals(hash, File("$workingDirectory/.kit/refs/heads/test").readText())

    }

    @Test
    fun `make a new branch without a commit`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        val exception = assertThrows<Exception> {
            branch("test")
        }
        assertEquals("fatal: Not a valid object name: 'master'.", exception.message)
    }


    @Test
    fun `set config`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        Config.set("user.name", "test")
        Config.set("user.email", "test@gmail.com")

        assertEquals("test", Config.get("user.name"))
        assertEquals("test@gmail.com", Config.get("user.email"))
    }

    @Test
    fun `get history one commit`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val hash = commit("test commit")
        val history = getHistory()
        assertEquals(hash, history[0])
        log()
    }


    @Test
    fun `get history multiple commit`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        val commits = mutableListOf<String>()
        for (i in 1..5) {
            // create a file
            val filePath = "${workingDirectory.absolutePathString()}/test.txt"
            File(filePath).writeText("test text $i")
            add(filePath)
            val hash = commit("test commit")
            commits.add(hash)
            if (i == 3) {
                branch("demo")
            }
            if (i == 4) {
                tag("v1.0.0", "Version 1.0.0")
            }
            if (i == 5) {
                tag("v1.0.1", "Version 1.0.1")
            }
        }
        val history = getHistory()
        for (i in 0..4) {
            assertEquals(commits[4 - i], history[i])
        }
        log()
    }

    @Test
    fun `get history multiple commit with Detached HEAD`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        val commits = mutableListOf<String>()
        for (i in 1..5) {
            // create a file
            val filePath = "${workingDirectory.absolutePathString()}/test.txt"
            File(filePath).writeText("test text $i")
            add(filePath)
            val hash = commit("test commit")
            commits.add(hash)
            if (i == 3) {
                branch("demo")
            }
            if (i == 5) {
                checkout(hash)
            }
        }
        val history = getHistory()
        for (i in 0..4) {
            assertEquals(commits[4 - i], history[i])
        }
        log()
    }

    @Test
    fun `get empty history`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        val history = getHistory()
        assertEquals(0, history.size)
    }

    @Test
    fun `create a tag`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        commit("test commit")
        val hash = tag("test", "test tag")
        assertEquals(hash, File("$workingDirectory/.kit/refs/tags/test").readText())
    }

    @Test
    fun `create an existing tag`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        commit("test commit")
        tag("test", "test tag")
        val exception = assertThrows<Exception> {
            tag("test", "test tag")
        }
        assertEquals("fatal: tag 'test' already exists", exception.message)
    }

    @Test
    fun `create a tag with directories`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        commit("test commit")
        val hash = tag("test/test", "test tag")
        assertEquals(hash, File("$workingDirectory/.kit/refs/tags/test/test").readText())
    }

    @Test
    fun `create tag with HEAD pointing to a commit`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.absolutePathString()}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        checkout(commit("test commit"))
        val hash = tag("test", "test tag")
        assertEquals(hash, File("$workingDirectory/.kit/refs/tags/test").readText())
    }

    @Test
    fun `create a tag on a commit other than HEAD`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        val commits = mutableListOf<String>()
        for (i in 1..5) {
            // create a file
            val filePath = "${workingDirectory.absolutePathString()}/test.txt"
            File(filePath).writeText("test text $i")
            add(filePath)
            val hash = commit("test commit")
            commits.add(hash)
        }
        val hash = tag("test", "test tag", commits[2])
        assertEquals(hash, File("$workingDirectory/.kit/refs/tags/test").readText())
    }

    @Test
    fun `create a tag without any commit`() {
        // create working directory
        val workingDirectory = Path.of("src/test/resources/workingDirectory").toAbsolutePath()
        workingDirectory.toFile().mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.toString())
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        val exception = assertThrows<Exception> {
            tag("test", "test tag")
        }
        assertEquals("fatal: Failed to resolve 'HEAD' as a valid ref.", exception.message)
    }

}
