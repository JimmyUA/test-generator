package com.github.jimmyua.testgenerator.action

import com.github.jimmyua.testgenerator.action.model.FileInfo
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.vfs.VfsUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

internal class GenerateTestAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {

        val fileData = extractData(e)
        val createdFile = createTestFile(fileData)
        val testContent = createTestData(fileData)

        createdFile.writeText(testContent);

        navigateToTest(createdFile, e)
    }

    private fun navigateToTest(createdFile: File, e: AnActionEvent) {
        val virtualFile = VfsUtil.findFile(createdFile.toPath(), true)
        val openFileDescriptor = OpenFileDescriptor(e.project!!, virtualFile!!)
        openFileDescriptor.navigate(true)
    }

    private fun extractFields(fileContent: String): List<String> {
        val braceIndex = fileContent.indexOf("{")
        val contentWithoutClass = fileContent.substring(braceIndex)
        val firstPIndex = contentWithoutClass.indexOf("p")
        val startFields = contentWithoutClass.substring(firstPIndex)


        return startFields.lines()
                .filter { it.contains("private final") || it.contains("protected final") }
                .map { it.trim() }
                .map { it.split(" ") }
                .map { it[2] + " " + it[3] }
                .toList()
    }

    private fun extractFieldExports(fileContent: String, fields: List<String>): List<String> {
        val braceIndex = fileContent.indexOf("{")
        val imports = fileContent.substring(0, braceIndex)

        val fieldTypes = fields
                .map { it.split(" ")[0] }
                .toList()

        return imports.lines()
                .filter { containsFieldType(fieldTypes, it) }
                .toList()
    }

    private fun containsFieldType(fieldTypes: List<String>, line: String): Boolean {
       return fieldTypes
                .any { line.contains(it) }
    }

    private fun extractData(e: AnActionEvent): FileInfo {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val filePath = file?.path
        val fileName = file?.name
        val fileContent = VfsUtil.loadText(file!!)

        val fields = extractFields(fileContent)
        val fieldExports = extractFieldExports(fileContent, fields)

        val fileNameWithoutExtension = fileName?.split(".")?.get(0)
        val testFileName = fileNameWithoutExtension + "Test.java"
        val testFilePath = filePath?.replace("/main/", "/test/")?.replace(fileName!!, "")


        return FileInfo(
                name = fileName!!.replace(".java", ""),
                fieldExports = fieldExports,
                testName = testFileName,
                path = testFilePath!!,
                fields = fields
        )
    }

    private fun createTestFile(fileInfo: FileInfo): File {
        Files.createDirectories(Paths.get(fileInfo.path))
        val file = File(fileInfo.path, fileInfo.testName)
        file.createNewFile()
        return file
    }

    private fun createTestData(fileData: FileInfo): String {

        val imports = fileData.fieldExports
                .joinToString(System.lineSeparator()) { import ->
                    """
                        $import
                """.trimIndent()
                }

        val fileContent = """
            package ${toPackageName(fileData.path)}
            
            import org.junit.Test;
            import org.junit.runner.RunWith;
            import org.mockito.InjectMocks;
            import org.mockito.Mock;
            import org.mockito.junit.MockitoJUnitRunner;
            
            $imports
            
            
            @RunWith(MockitoJUnitRunner.class)
            public class ${fileData.testName.replace(".java", "")} {
            
              @InjectMocks
              private ${fileData.name} ${toFieldName(fileData.name)};
              
        """.trimIndent()

        val mocks = fileData.fields
                .joinToString(System.lineSeparator()) { field ->
                    """
                      @Mock
                      private $field
                """.trimIndent()
                }

        val classEnd = """
            
              }
        """.trimIndent()

        return fileContent + mocks + classEnd
    }

    private fun toFieldName(className: String): String {
        val firstChar = className[0].toLowerCase()
        return firstChar + className.substring(1)
    }

    private fun toPackageName(path: String): String {
        val packageName = path.substring(path.indexOf("java/") + 5, path.length - 1)
        return packageName.replace("/", ".") + ";"
    }
}
