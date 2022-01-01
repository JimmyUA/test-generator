package com.github.jimmyua.testgenerator.services

import com.intellij.openapi.project.Project
import com.github.jimmyua.testgenerator.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
