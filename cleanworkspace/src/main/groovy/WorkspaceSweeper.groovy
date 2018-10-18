import hudson.model.*;
import hudson.util.*;
import hudson.FilePath;
import jenkins.model.*;
import hudson.FilePath.FileCallable;
import hudson.slaves.OfflineCause;
import hudson.node_monitors.*;
import com.cloudbees.hudson.plugins.folder.*
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject

def parsedJob = [:]
def runningJob = [:]

def printNodeFreeSpace(node) {

    computer = node.toComputer()
    rootPath = node.getRootPath()
    size = DiskSpaceMonitor.DESCRIPTOR.get(computer).size
    roundedSize = size / (1024 * 1024 * 1024) as int

    nodeName = node.getDisplayName()
    println("node: " + nodeName + ", free space: " + roundedSize + "GB")
}

def getNode(job) {
    if(job.getAssignedLabel() == null) {
        return Jenkins.getInstance()
    }
    for (nodeSlave in Jenkins.instance.nodes) {
        if(nodeSlave.getDisplayName().toString() == job.getAssignedLabel().toString()) {
            return nodeSlave;
        }
    }
    return null
}

def cleanUnusedWorspace(node, Map parsedJobMap, Map runningJobMap){

    println "Clean unused workspaces in  $node.displayName"

    def FilePath workspaceRoot = node.rootPath.child("workspace");

    workspaceRoot.listDirectories().each { child ->
        worspaceFullName = workspaceRoot.child(child.name)

        def doWipe = true

        if( parsedJobMap.containsKey(node.displayName)){
            //println "Already cleand node: " + node.displayName + " . Content: " + parsedJobMap.get(node.displayName).toString()
            if(parsedJobMap.get(node.displayName).containsKey(worspaceFullName.getRemote())){
                // the path match a job we have already cleaned up the workcpace
                //println "Already cleaned workspaces: " + worspaceFullName.getRemote()
                doWipe = false
            }
        }

        if( child.name.indexOf("@") > 0){
            //  workspace create for a job in multi run
            def childPrefixName = child.name.substring(0, child.name.indexOf("@"))

            if( runningJobMap.containsKey(node.displayName)){
                worspaceRealFullName = workspaceRoot.child(childPrefixName)
                if(runningJobMap.get(node.displayName).containsKey(worspaceRealFullName.getRemote())){
                    // the path match a job that is running
                    //println "Job i running: " + worspaceRealFullName.getRemote()
                    doWipe = false
                }
            }
        }



        if (doWipe) {
            println "Deleting: " + worspaceFullName.getRemote() + " At node: " + node.displayName
            child.deleteRecursive()
        }
    }
}


printNodeFreeSpace(Jenkins.getInstance()) // master
for (nodeSlave in Jenkins.instance.nodes) {
    printNodeFreeSpace(nodeSlave)

}


for (item in Jenkins.instance.items) {
    print(item.getFullDisplayName() + " ")
    if(item instanceof Folder){
        println("Folder skipped")
        continue
    }
    if(item instanceof WorkflowMultiBranchProject){
        println("WorkflowMultiBranchProject skipped")
        continue
    }

    jobName = item.getFullDisplayName()
    nodeName = item.getAssignedLabel()==null?"Master":item.getAssignedLabel().toString()


    node = getNode(item)
    if(node == null) {
        println("Unkown node " + item.getAssignedLabel())
        continue
    }

    workspacePath = node.getWorkspaceFor(item)
    if (workspacePath == null) {
        println(" ... could not get workspace path")
        continue
    }

    customWorkspace = item.getCustomWorkspace()
    if (customWorkspace != null) {
        workspacePath = node.getRootPath().child(customWorkspace)
        print(" (custom workspace)")
    }

    pathAsString = workspacePath.getRemote()

    if( !parsedJob.containsKey(node.displayName)) {
        parsedJob.put(node.displayName, [:])
    }
    parsedJob.get(node.displayName).putAt(pathAsString , item.fullDisplayName)


    if (item.isBuilding()) {
        println("Job is currently running, skipped")
        if( !runningJob.containsKey(node.displayName)) {
            runningJob.put(node.displayName, [:])
        }
        runningJob.get(node.displayName).putAt(pathAsString, item.fullDisplayName)

        continue
    }

    print("[" + nodeName + "] " + pathAsString)
    if (workspacePath.exists()) {
        try {
            workspacePath.deleteRecursive()

        } catch(exception) {
            println(" - failed : " + exception)
        }
        println(" - deleted")
    } else {
        println(" - nothing to delete")
    }

    // trigger re-calculation of workspace disk usage
    if( item instanceof AbstractProject) {

        def AbstractProject projectItem = item
        for (WorkspaceListener wl : WorkspaceListener.all()) {
            wl.afterDelete(projectItem)
        }
    }
}

// wipe unused workspaces (orphan)
cleanUnusedWorspace(Jenkins.getInstance(), parsedJob, runningJob) // master
for (node in Jenkins.instance.nodes) {
    cleanUnusedWorspace(node, parsedJob, runningJob)
}
