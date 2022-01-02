package com.github.jimmyua.testgenerator.action.model

data class FileInfo(
        val name: String,
        val path: String,
        val testName: String,
        val fields: List<String>,
        val fieldExports: List<String>
)
