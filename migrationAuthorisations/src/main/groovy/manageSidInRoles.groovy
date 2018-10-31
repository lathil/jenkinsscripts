import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import com.michelin.cio.hudson.plugins.rolestrategy.RoleMap
import hudson.security.AuthorizationStrategy
import jenkins.model.Jenkins


def unassignSidFromGlobalRole(String sid, String roleName) {

    AuthorizationStrategy strategy = Jenkins.get().getAuthorizationStrategy()
    if (strategy != null && strategy instanceof RoleBasedAuthorizationStrategy) {


        RoleMap roleMap = strategy.getGrantedRoles(RoleBasedAuthorizationStrategy.GLOBAL)
        roleMap.unAssignRole(roleMap.getRole(roleName), sid)

        Jenkins.get().save()
    }
}

def assignSidToGlobalRole(String sid, String roleName) {
    AuthorizationStrategy strategy = Jenkins.get().getAuthorizationStrategy()
    if (strategy != null && strategy instanceof RoleBasedAuthorizationStrategy) {


        RoleMap roleMap = strategy.getGrantedRoles(RoleBasedAuthorizationStrategy.GLOBAL)
        roleMap.assignRole(roleMap.getRole(roleName), sid)

        Jenkins.get().save()
    }
}

assignSidToGlobalRole('bbauzon', 'admin')
//unassignSidFromGlobalRole('lthil','admin')

