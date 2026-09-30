package com.example.audioplayer

sealed class FileItem {
    abstract val name: String
    abstract val path: String

    data class Folder(
        override val name: String,
        override val path: String
    ) : FileItem()

    data class Audio(
        override val name: String,
        override val path: String
    ) : FileItem()
}
