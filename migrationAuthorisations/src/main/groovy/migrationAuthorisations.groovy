import hudson.model.AbstractItem
import hudson.model.AbstractProject
import hudson.model.Job
import jenkins.branch.MultiBranchProject
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import jenkins.model.Jenkins
import hudson.security.AuthorizationMatrixProperty
import hudson.security.AuthorizationStrategy
import hudson.security.Permission
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import com.michelin.cio.hudson.plugins.rolestrategy.Role
import com.michelin.cio.hudson.plugins.rolestrategy.RoleMap

import java.lang.reflect.*
import java.util.regex.Matcher
import java.util.regex.Pattern


def getProjectPermissions( item) {

    HashMap<String, Set<Permission>> rperm = new HashMap<String, List<Permission>>()
    AuthorizationMatrixProperty authProperties = null
    if( item instanceof  AbstractProject){
        authProperties = ((AbstractProject)item).getProperty(AuthorizationMatrixProperty.class)
    } else if( item instanceof Job) {
        authProperties = ((Job)item).getProperty(AuthorizationMatrixProperty.class)
    }

    authProperties?.grantedPermissions?.each { permission, granted  ->
        granted.each { id ->
            if( rperm.containsKey( id)){
                if(!rperm.get(id).contains(permission)){rperm.get(id).add(permission)}
            } else {
                def perms = new HashSet<Permission>()
                perms.add(permission)
                rperm.put(id, perms)
            }
        }
    }

    rperm.each {id, perms ->
        println("Job name: ${item.name} Sid: ${id} " + perms.each {k -> k.name + ", "})
    }

}

def checkGlobalRole(RoleBasedAuthorizationStrategy roleStrategy, String roleName, Set<Permission> permissions){
    SortedMap<Role, Set<String>> roles = roleStrategy.getGrantedRoles(RoleBasedAuthorizationStrategy.GLOBAL)
    def entry = roles.find { it.key.name == roleName }
    Role role = null
    if( entry == null) {
        // role does not exist, create it.
        role = new Role(roleName, permissions)
        println("Create global role: ${roleName}")
        roleStrategy.addRole(RoleBasedAuthorizationStrategy.GLOBAL, role)
    } else {
        println("Find role: ${entry.key.name}")
        role = entry.key
    }

    return role
}

def checkProjectRole(RoleBasedAuthorizationStrategy roleStrategy,  String trigramm, String roleSuffix, Set<Permission> permissions){
    def String roleName = trigramm + "-" + roleSuffix
    SortedMap<Role, Set<String>> roles = roleStrategy.getGrantedRoles(RoleBasedAuthorizationStrategy.PROJECT)
    def entry = roles.find { it.key.name == roleName }
    Role role = null
    if( entry == null) {
        // role does not exist, create it.
        String pattern = "(?i)^${trigramm}[\\-_].*"
        role = new Role(roleName, pattern, permissions)
        println("Create role: ${roleName}, pattern: ${pattern}")
        roleStrategy.addRole(RoleBasedAuthorizationStrategy.PROJECT, role)
    } else {
        println("Find role: ${entry.key.name}")
        role = entry.key
    }

    return role
}

// Role construtot not accessible, make it so.
Constructor[] constrs = Role.class.getConstructors()
for (Constructor<?> c : constrs) {
    c.setAccessible(true)
}
// Make the method assignRole accessible
Method assignRoleMethod = RoleBasedAuthorizationStrategy.class.getDeclaredMethod("assignRole", String.class, Role.class, String.class);
assignRoleMethod.setAccessible(true);

def buildSuffix = "build"
def advUserSuffix = "advuser"
def userSuffix = "user"
def consultSuffix = "consult"

Set<Permission> globalReadPermissionSet = new HashSet<Permission>()
globalReadPermissionSet.add(Permission.fromId("hudson.model.Hudson.Read"))

Set<Permission> globalCreatePermissionSet = new HashSet<Permission>()
globalCreatePermissionSet.add(Permission.fromId("hudson.model.Item.Create"))

Set<Permission> projectBuildPermissionSet = new HashSet<Permission>()
projectBuildPermissionSet.add(Permission.fromId("hudson.model.Item.Build"))

Set<Permission> consultPermissionSet =  new HashSet<Permission>()
consultPermissionSet.add(Permission.fromId("hudson.model.Item.Read"))
consultPermissionSet.add(Permission.fromId("hudson.model.Item.Workspace"))
consultPermissionSet.add(Permission.fromId("hudson.model.View.Read"))

Set<Permission> userPermissionSet = new HashSet<Permission>()
userPermissionSet.add(Permission.fromId("hudson.plugins.promoted_builds.Promotion.Promote"));
userPermissionSet.add(Permission.fromId("hudson.plugins.release.ReleaseWrapper.Release"));
userPermissionSet.add(Permission.fromId("hudson.model.Item.Configure"));
userPermissionSet.add(Permission.fromId("hudson.model.Item.Cancel"));
userPermissionSet.add(Permission.fromId("hudson.model.Item.Read"));
userPermissionSet.add(Permission.fromId("hudson.model.Item.Delete"));
userPermissionSet.add(Permission.fromId("hudson.model.Item.Build"));
userPermissionSet.add(Permission.fromId("hudson.model.Item.Discover"));
userPermissionSet.add(Permission.fromId("hudson.model.Item.Move"));
userPermissionSet.add(Permission.fromId("hudson.model.Item.Release"));
userPermissionSet.add(Permission.fromId("hudson.model.Item.Workspace"));
userPermissionSet.add(Permission.fromId("hudson.model.Item.ExtendedRead"));
userPermissionSet.add(Permission.fromId("hudson.model.View.Configure"));
userPermissionSet.add(Permission.fromId("hudson.model.View.Read"));
userPermissionSet.add(Permission.fromId("hudson.model.Run.Delete"));
userPermissionSet.add(Permission.fromId("hudson.model.Run.Update"));
userPermissionSet.add(Permission.fromId("hudson.scm.SCM.Tag"));

Set<Permission> advuserPermissionSet = new HashSet<Permission>()
advuserPermissionSet.add(Permission.fromId("hudson.plugins.promoted_builds.Promotion.Promote"));
advuserPermissionSet.add(Permission.fromId("hudson.plugins.release.ReleaseWrapper.Release"));
advuserPermissionSet.add(Permission.fromId("hudson.model.Item.Configure"));
advuserPermissionSet.add(Permission.fromId("hudson.model.Item.Cancel"));
advuserPermissionSet.add(Permission.fromId("hudson.model.Item.Read"));
advuserPermissionSet.add(Permission.fromId("hudson.model.Item.Delete"));
advuserPermissionSet.add(Permission.fromId("hudson.model.Item.Build"));
advuserPermissionSet.add(Permission.fromId("hudson.model.Item.Discover"));
advuserPermissionSet.add(Permission.fromId("hudson.model.Item.Create"));
advuserPermissionSet.add(Permission.fromId("hudson.model.Item.Move"));
advuserPermissionSet.add(Permission.fromId("hudson.model.Item.Release"));
advuserPermissionSet.add(Permission.fromId("hudson.model.Item.Workspace"));
advuserPermissionSet.add(Permission.fromId("hudson.model.Item.ExtendedRead"));
advuserPermissionSet.add(Permission.fromId("hudson.model.View.Configure"));
advuserPermissionSet.add(Permission.fromId("hudson.model.View.Create"));
advuserPermissionSet.add(Permission.fromId("hudson.model.View.Read"));
advuserPermissionSet.add(Permission.fromId("hudson.model.View.Delete"));
advuserPermissionSet.add(Permission.fromId("hudson.model.Run.Delete"));
advuserPermissionSet.add(Permission.fromId("hudson.model.Run.Update"));
advuserPermissionSet.add(Permission.fromId("hudson.scm.SCM.Tag"));

String projectNameRegEx = "^([a-zA-Z0-9]{3})[\\-_].*"
Pattern pattern = Pattern.compile(projectNameRegEx);

AuthorizationStrategy strategy = Jenkins.get().getAuthorizationStrategy()
if (strategy != null && strategy instanceof RoleBasedAuthorizationStrategy) {

    RoleBasedAuthorizationStrategy roleStrategy = strategy as RoleBasedAuthorizationStrategy

    Set<String> globalUsers = roleStrategy.getSIDs(RoleBasedAuthorizationStrategy.GLOBAL)

    // authenticated should be allow to create by default
    def globalCreatorRole = checkGlobalRole(strategy, 'globalCreator', globalCreatePermissionSet)
    strategy.assignRole(RoleBasedAuthorizationStrategy.GLOBAL, globalCreatorRole, 'authenticated')

    // anonymous should have accès to read globaly
    def globalReaderRole = checkGlobalRole(strategy, 'globalReader', globalReadPermissionSet)
    strategy.assignRole(RoleBasedAuthorizationStrategy.GLOBAL, globalReaderRole, 'anonymous')

    Jenkins.get().getItems(AbstractItem).each { item ->
        // check that name matches trigram pattern
        Matcher matcher = pattern.matcher(item.name)
        if (matcher.matches() && matcher.groupCount() == 1) {
            def trigram = matcher.group(1).toLowerCase()
            // create default rôles for the trigrame if they do not exist yet
            def roleuser = checkProjectRole(strategy, trigram, userSuffix, userPermissionSet)
            def roleadv = checkProjectRole(strategy, trigram, advUserSuffix, advuserPermissionSet)
            def roleconsult = checkProjectRole(strategy, trigram, consultSuffix, consultPermissionSet)

            // assign default roles to accepted ad groups
            strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleadv, "PGG_APPLI_DEVOPS_DEV_${trigram.toUpperCase()}_ADVANCED_USER")
            strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleuser, "PGG_APPLI_DEVOPS_DEV_${trigram.toUpperCase()}_USER")
            strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleconsult, "PGG_APPLI_DEVOPS_DEV_${trigram.toUpperCase()}_CONSULTATION")

            HashMap<String, Set<Permission>> permissions = getProjectPermissions(item)
            permissions.each { k, v ->
                if (!k.equals('anonymous')) {
                    println("Importing user: ${k} in project roles")
                    boolean hasReleaseWrapper = v.contains(Permission.fromId("hudson.plugins.release.ReleaseWrapper.Release"))
                    boolean hasRelease = v.contains(Permission.fromId("hudson.model.Item.Release"))
                    boolean hasJobCreate = v.contains(Permission.fromId("hudson.model.Item.Create"))
                    boolean hasViewCreate = v.contains(Permission.fromId("hudson.model.View.Create"))
                    println("hasReleaseWrapper: ${hasReleaseWrapper}, hasRelease: ${hasRelease}, hasJobCreate: ${hasJobCreate}, hasViewCreate: ${hasViewCreate}")
                    if( hasViewCreate || hasJobCreate) {
                        println("User: ${k} is project advance user")
                        strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleadv, k)
                    } else {
                        println("User ${k} is project user")
                        strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleuser, k)
                    }

                } else {
                    println("User: ${k} already in global users list")
                }
            }

            if( trigram.equals("xld")){
                // user should be able to launch and read logs from xld jobs
                def rolebuild = checkProjectRole(strategy, trigram, buildSuffix, projectBuildPermissionSet)
                strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, rolebuild, "authenticated")

                strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleconsult, "authenticated")
            }

        } else if (item.name.startsWith("Template")){
            // Templates should have public read access and keep existing user rights
            def roleconsult = checkProjectRole(strategy, 'Template', consultSuffix, consultPermissionSet)
            strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleconsult, "authenticated")

            def roleadv = checkProjectRole(strategy, 'Template', advUserSuffix, advuserPermissionSet)
            HashMap<String, Set<Permission>> permissions = getProjectPermissions(item)
            permissions.each { k, v ->
                println("User: ${k} is project advance user")
                strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleadv, k)
            }
        }
    }

    Jenkins.get().save()
}

