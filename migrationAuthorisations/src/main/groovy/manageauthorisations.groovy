import com.michelin.cio.hudson.plugins.rolestrategy.Role
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import com.michelin.cio.hudson.plugins.rolestrategy.RoleMap
import hudson.security.AuthorizationStrategy
import jenkins.model.Jenkins


def unassignSidFromProject( String trigram) {


    AuthorizationStrategy strategy = Jenkins.get().getAuthorizationStrategy()
    if (strategy != null && strategy instanceof RoleBasedAuthorizationStrategy) {


        RoleMap rolesMap = strategy.getGrantedRoles(RoleBasedAuthorizationStrategy.PROJECT)
        Set<String> sidsConsult = rolesMap.getSidsForRole(trigram + "-consult")
        def sidsUser = rolesMap.getSidsForRole(trigram + "-user")
        def sidsAdvUser = rolesMap.getSidsForRole(trigram + "-advuser")
        println("Sids for ${trigram}: " + sidsConsult + ", " + sidsUser + ", " + sidsAdvUser)

        def consultMap = rolesMap.getRole(trigram + "-consult")
        if (consultMap != null) {
            rolesMap.clearSidsForRole(consultMap)
        }
        def userMap = rolesMap.getRole(trigram + "-user")
        if (userMap != null) {
            rolesMap.clearSidsForRole(userMap)
        }
        def advmap = rolesMap.getRole(trigram + "-advuser")
        if( advmap != null) {
            rolesMap.clearSidsForRole(advmap)
        }

        Jenkins.get().save()
    }
}
def unassignSidFromView( String trigram) {

    AuthorizationStrategy strategy = Jenkins.get().getAuthorizationStrategy()
    if (strategy != null && strategy instanceof RoleBasedAuthorizationStrategy) {

        RoleMap rolesMap = strategy.getGrantedRoles(RoleBasedAuthorizationStrategy.VIEW)

        def consultMap = rolesMap.getRole(trigram )
        if (consultMap != null) {
            rolesMap.clearSidsForRole(consultMap)
        }

        Jenkins.get().save()
    }
}

unassignSidFromProject("ado")
unassignSidFromProject("evp")
unassignSidFromProject("evb")
unassignSidFromProject("mab")
unassignSidFromProject("dvo")

unassignSidFromProject("apc")
unassignSidFromProject("ctr")
unassignSidFromProject("dpo")
unassignSidFromProject("gde")
unassignSidFromProject("gtv")
unassignSidFromProject("idn")
unassignSidFromProject("idr")
unassignSidFromProject("ins")
unassignSidFromProject("mc2")

unassignSidFromProject("rcu")
unassignSidFromProject("spa")
unassignSidFromProject("eit")

unassignSidFromView("ado")
unassignSidFromView("evp")
unassignSidFromView("evb")
unassignSidFromView("mab")
unassignSidFromView("dvo")

unassignSidFromView("apc")
unassignSidFromView("ctr")
unassignSidFromView("dpo")
unassignSidFromView("gde")
unassignSidFromView("gtv")
unassignSidFromView("idn")
unassignSidFromView("idr")
unassignSidFromView("ins")
unassignSidFromView("mc2")

unassignSidFromView("rcu")
unassignSidFromView("spa")
unassignSidFromView("eit")



