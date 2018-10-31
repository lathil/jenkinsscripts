import hudson.model.Job
import hudson.security.Permission
import jenkins.model.Jenkins

import java.lang.reflect.Method
import java.util.regex.Matcher
import java.util.regex.Pattern

import hudson.security.ProjectMatrixAuthorizationStrategy
import hudson.security.AuthorizationMatrixProperty

String projectNameRegEx = "^testmigrationfrom_[0-9]{1,2}.[0-9]_(.*)"
Pattern pattern = Pattern.compile(projectNameRegEx)

// Make the method assignRole accessible
Method add = AuthorizationMatrixProperty.class.getDeclaredMethod("add", Permission.class, String.class)
add.setAccessible(true);

//AuthorizationStrategy strategy = Jenkins.get().getAuthorizationStrategy()
ProjectMatrixAuthorizationStrategy authorizationStrategy = (ProjectMatrixAuthorizationStrategy) Jenkins.get().getAuthorizationStrategy()

Jenkins.get().getItems(Job).each { item ->
    // check that name matches trigram pattern
    Matcher matcher = pattern.matcher(item.name)
    if (matcher.matches() && matcher.groupCount() == 1) {
        println("projet migration: ${item.name} , projet migré: ${matcher.group(1)}")


        Job sourceJob = Jenkins.get().getItem(matcher.group(1))
        if ( sourceJob != null) {
            AuthorizationMatrixProperty sourceMatrix = sourceJob.getProperty(AuthorizationMatrixProperty.class)
            //println("Source job ${matcher.group(1)} has auth matrix: " + (sourceMatrix != null ? "yes" : "no"))

            AuthorizationMatrixProperty destinationMatrix = item.getProperty(AuthorizationMatrixProperty.class)
            //println("Destination job ${item.name} has auth matrix: " + (destinationMatrix != null ? "yes" : "no"))

            if(destinationMatrix == null){
                destinationMatrix  = new AuthorizationMatrixProperty()

                for( Map.Entry<Permission, Set<String>>  entry : sourceMatrix.grantedPermissions ){
                    Permission permission = (Permission) entry.key
                    for( String sid : entry.value){
                        //println ("Check permission: ${permission.name}, sid: ${sid}")
                        if( !destinationMatrix.hasExplicitPermission(sid, permission)){
                            destinationMatrix.add(permission, sid)
                            //println("Copy  permissions from ${matcher.group(1)} to : ${item.name}")
                        }
                    }
                }

                println("Copy  permissions from ${matcher.group(1)} to : ${item.name}")
                //item.addProperty(destinationMatrix)
            }
        } else {
            println("Couldn't not find source job: ${matcher.group(1)}")
        }
    }
}
