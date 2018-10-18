import hudson.model.AbstractItem
import hudson.model.AbstractProject
import jenkins.branch.MultiBranchProject
import jenkins.model.Jenkins
import hudson.model.Job

import java.util.regex.Pattern



String projectNameRegEx = "^[a-zA-Z0-9]{3}[\\-_].*"
Pattern pattern = Pattern.compile(projectNameRegEx);


def items = Jenkins.get().getItems(AbstractItem)
println("Total items: ${items.size()}")
items.each { item ->
    if (!pattern.matcher(item.name).matches()) {
        println("item name: ${item.name} , item class: ${item.class.name}")
    }
}

// get FreeStyle and Maven projects
def projects = Jenkins.get().getItems(AbstractProject)
println("Total projects: ${projects.size()}")
projects.each { project ->
    if (!pattern.matcher(project.name).matches()) {
        println("project name: ${project.name} , project class: ${project.class.name}")
    }
}

def jobs  = Jenkins.get().getItems(Job)
println("Total jobs: ${jobs.size()}")
jobs.each { job ->
    if (!pattern.matcher(job.name).matches()) {
        println("job name: ${job.name}, project class: ${job.class.name}")
    }
}

def multibranches = Jenkins.get().getItems(MultiBranchProject)
println("Total multibranchess: ${multibranches.size()}")
multibranches.each { multibranche ->
    if (!pattern.matcher(multibranche.name).matches()) {
        println("multibranche name: ${multibranche.name}, project class: ${multibranche.class.name}")
    }
}