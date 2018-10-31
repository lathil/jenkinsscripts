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

/**
 * Get the permissions assigned to an sid for a project by the matrix authorisation strategy plugin
 * @param item the name of the project
 * @return HashMap<String, Set<Permissions>>
 */
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

/**
 * Check that global role existe, if not create it
 * @param roleStrategy
 * @param roleName
 * @param permissions
 * @return role
 */
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

/**
 * Check that project role exist, if not, create it
 * @param roleStrategy
 * @param trigramm
 * @param roleSuffix
 * @param permissions
 * @return role
 */
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

/**
 * Check that a project role based on regex with capturing groups exists.
 * @param roleStrategy
 * @param roleName
 * @param permissions
 * @return
 */
def checkRegexProjectRole( RoleBasedAuthorizationStrategy roleStrategy, String roleName, Set<Permission> permissions){
    SortedMap<Role, Set<String>> roles = roleStrategy.getGrantedRoles(RoleBasedAuthorizationStrategy.PROJECT)
    def entry = roles.find { it.key.name == roleName }
    Role role = null
    if( entry == null) {
        // role does not exist, create it.
        String pattern = "^([a-zA-Z0-9]{3})[\\-_].*"
        role = new Role(roleName, pattern, permissions)
        println("Create role: ${roleName}, pattern: ${pattern}")
        roleStrategy.addRole(RoleBasedAuthorizationStrategy.PROJECT, role)
    } else {
        println("Find role: ${entry.key.name}")
        role = entry.key
    }

    return role
}

/**
 * Check that view role exists, if not create it
 * @param roleStrategy
 * @param trigramm
 * @param permissions
 * @return role
 */
def checkViewRole(RoleBasedAuthorizationStrategy roleStrategy, String trigramm,  Set<Permission> permissions) {
    def String roleName = trigramm ;
    SortedMap<Role, Set<String>> roles = roleStrategy.getGrantedRoles(RoleBasedAuthorizationStrategy.VIEW)
    def entry = roles.find { it.key.name == roleName }
    Role role = null
    if( entry == null) {
        // role does not exist, create it.
        String pattern = "(?i)^${trigramm}"
        role = new Role(roleName, pattern, permissions)
        println("Create role: ${roleName}, pattern: ${pattern}")
        roleStrategy.addRole(RoleBasedAuthorizationStrategy.VIEW, role)
    } else {
        println("Find role: ${entry.key.name}")
        role = entry.key
    }

    return role
}

/**
 * Check that a view role based on a pattern with capturing groups exists, create it otherwise
 * @param roleStrategy
 * @param roleName
 * @param permissions
 * @return
 */
def checkRegExViewRole(RoleBasedAuthorizationStrategy roleStrategy, String roleName,  Set<Permission> permissions) {
    SortedMap<Role, Set<String>> roles = roleStrategy.getGrantedRoles(RoleBasedAuthorizationStrategy.VIEW)
    def entry = roles.find { it.key.name == roleName }
    Role role = null
    if( entry == null) {
        // role does not exist, create it.
        String pattern = "^([a-zA-Z0-9]{3})[\\-_]?.*"
        role = new Role(roleName, pattern, permissions)
        println("Create role: ${roleName}, pattern: ${pattern}")
        roleStrategy.addRole(RoleBasedAuthorizationStrategy.VIEW, role)
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

// consultation set of permissions
Set<Permission> consultPermissionSet =  new HashSet<Permission>()
consultPermissionSet.add(Permission.fromId("hudson.model.Item.Read"))
consultPermissionSet.add(Permission.fromId("hudson.model.Item.Workspace"))
consultPermissionSet.add(Permission.fromId("hudson.model.View.Read"))
// user set of permissions
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
userPermissionSet.add(Permission.fromId("hudson.model.View.Read"));
userPermissionSet.add(Permission.fromId("hudson.model.Run.Delete"));
userPermissionSet.add(Permission.fromId("hudson.model.Run.Update"));
userPermissionSet.add(Permission.fromId("hudson.scm.SCM.Tag"));
// advance user set of permissions
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
// view permissions for all
Set<Permission> viewPermissionSet = new HashSet<Permission>()
viewPermissionSet.add(Permission.fromId("hudson.model.View.Read"))
viewPermissionSet.add(Permission.fromId("hudson.model.View.Configure"))

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
    strategy.assignRole(RoleBasedAuthorizationStrategy.GLOBAL, globalReaderRole, 'authenticated')

    def roleuser = checkRegexProjectRole(strategy, "PGG_APPLI_TRIGRAM_DEV_USER", userPermissionSet)
    def roleadv = checkRegexProjectRole(strategy, "PGG_APPLI_TRIGRAM_DEV_ADVANCED_USER", advuserPermissionSet)
    def roleconsult = checkRegexProjectRole(strategy, "PGG_APPLI_TRIGRAM_DEV_ADVANCED_CONSULTATION", consultPermissionSet)
    def roleviewaccess = checkRegExViewRole(strategy, "PGG_APPLI_TRIGRAM_DEV_VIEWER", viewPermissionSet)

    strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleadv, "^PGG_APPLI_TRIGRAM_DEV_([a-zA-Z0-9]{3})_ADMIN")
    strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleadv, "^PGG_APPLI_TRIGRAM_DEV_([a-zA-Z0-9]{3})_ADVANCED_USER")
    strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleuser, "^PGG_APPLI_TRIGRAM_DEV_([a-zA-Z0-9]{3})_USER")
    strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleconsult, "^PGG_APPLI_TRIGRAM_DEV_([a-zA-Z0-9]{3})_CONSULTATION")

    strategy.assignRole(RoleBasedAuthorizationStrategy.VIEW, roleviewaccess, "^PGG_APPLI_TRIGRAM_DEV_([a-zA-Z0-9]{3})_ADMIN")
    strategy.assignRole(RoleBasedAuthorizationStrategy.VIEW, roleviewaccess, "^PGG_APPLI_TRIGRAM_DEV_([a-zA-Z0-9]{3})_ADVANCED_USER")
    strategy.assignRole(RoleBasedAuthorizationStrategy.VIEW, roleviewaccess, "^PGG_APPLI_TRIGRAM_DEV_([a-zA-Z0-9]{3})_USER")
    strategy.assignRole(RoleBasedAuthorizationStrategy.VIEW, roleviewaccess, "^PGG_APPLI_TRIGRAM_DEV_([a-zA-Z0-9]{3})_CONSULTATION")


    Jenkins.get().getItems(AbstractItem).each { item ->
        // check that name matches trigram pattern
        Matcher matcher = pattern.matcher(item.name)
        if (matcher.matches() && matcher.groupCount() == 1) {
            def trigram = matcher.group(1).toLowerCase()
            // create default rôles for the trigrame if they do not exist yet
            def roleusermig = checkProjectRole(strategy, trigram, userSuffix, userPermissionSet)
            def roleadvmig = checkProjectRole(strategy, trigram, advUserSuffix, advuserPermissionSet)
            def roleviewaccessmig = checkViewRole(strategy, trigram, viewPermissionSet)

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
                        strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleadvmig, k)
                    } else {
                        println("User ${k} is project user")
                        strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleusermig, k)
                    }
                    strategy.assignRole(RoleBasedAuthorizationStrategy.VIEW, roleviewaccessmig, k)

                } else {
                    println("User: ${k} already in global users list")
                }
            }

            if( trigram.equals("xld")){
                // user should be able to launch and read logs from xld jobs
                def rolebuildmig = checkProjectRole(strategy, trigram, buildSuffix, projectBuildPermissionSet)
                strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, rolebuildmig, "authenticated")
                def roleconsultmig = checkProjectRole(strategy, trigram, userSuffix, consultPermissionSet)
                strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleconsultmig, "authenticated")

                strategy.assignRole(RoleBasedAuthorizationStrategy.VIEW, roleviewaccessmig, "authenticated")
            }

        } else if (item.name.startsWith("Template")){
            // Templates should have public read access and keep existing user rights
            def roleconsultmig = checkProjectRole(strategy, 'Template', consultSuffix, consultPermissionSet)
            strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleconsultmig, "authenticated")

            def roleadvmig = checkProjectRole(strategy, 'Template', advUserSuffix, advuserPermissionSet)
            HashMap<String, Set<Permission>> permissions = getProjectPermissions(item)
            permissions.each { k, v ->
                println("User: ${k} is project advance user")
                strategy.assignRole(RoleBasedAuthorizationStrategy.PROJECT, roleadvmig, k)
            }
        }
    }

    Jenkins.get().save()
}

