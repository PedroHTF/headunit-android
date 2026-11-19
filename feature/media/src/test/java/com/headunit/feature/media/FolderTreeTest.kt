package com.headunit.feature.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderTreeTest {
    private val folders = listOf("Musicas/Rock", "Musicas/Rock/Anos 80", "Musicas/MPB", "Videos", "", "HeadUnit")

    @Test
    fun rootListsTopFolders() {
        assertEquals(listOf("HeadUnit", "Musicas", "Videos"), FolderTree.subfolders(folders, ""))
    }

    @Test
    fun nestedFolders() {
        assertEquals(listOf("MPB", "Rock"), FolderTree.subfolders(folders, "Musicas"))
        assertEquals(listOf("Anos 80"), FolderTree.subfolders(folders, "/Musicas/Rock/"))
        assertEquals(emptyList<String>(), FolderTree.subfolders(folders, "Videos"))
    }

    @Test
    fun parentAndJoin() {
        assertEquals("Musicas", FolderTree.parent("Musicas/Rock"))
        assertEquals("", FolderTree.parent("Musicas"))
        assertNull(FolderTree.parent(""))
        assertEquals("Musicas/Rock", FolderTree.join("Musicas", "Rock"))
        assertEquals("Videos", FolderTree.join("", "Videos"))
    }

    @Test
    fun countIncludesSubfolders() {
        assertEquals(3, FolderTree.countUnder(folders, "Musicas"))
        assertEquals(1, FolderTree.countUnder(folders, "Musicas/MPB"))
    }
}
