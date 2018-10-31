import com.michelin.cio.hudson.plugins.rolestrategy.Role
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import com.michelin.cio.hudson.plugins.rolestrategy.RoleMap
import hudson.security.AuthorizationStrategy
import jenkins.model.Jenkins


def unassignSid( String trigram) {
    String adGroupAdvUser = "PGG_APPLI_DEVOPS_DEV_${trigram.toUpperCase()}_ADVANCED_USER"
    String adGroupUser = "PGG_APPLI_DEVOPS_DEV_${trigram.toUpperCase()}_USER"
    String adGroupConsult = "PGG_APPLI_DEVOPS_DEV_${trigram.toUpperCase()}_CONSULTATION"

    String cdsSodifranceGrouAD = "CDS_IDD_SODIFRANCE"
    String groupEDV ="EDV"
    String groupMFI =  "MFI"
    String groupPGGTeam = "PGG_Team_STR"
    String groupTmealse = "TMEALARE"
    String groupeTmeLare = "TMEALARE"
    String groupeVTedongm = "VTEDONGM"

    AuthorizationStrategy strategy = Jenkins.get().getAuthorizationStrategy()
    if (strategy != null && strategy instanceof RoleBasedAuthorizationStrategy) {


        RoleMap rolesMap = strategy.getGrantedRoles(RoleBasedAuthorizationStrategy.PROJECT)
        Set<String> sidsConsult = rolesMap.getSidsForRole(trigram + "-consult")
        def sidsUser = rolesMap.getSidsForRole(trigram + "-user")
        def sidsAdvUser = rolesMap.getSidsForRole(trigram + "-advuser")
        println( "Sids for ${trigram}: " + sidsConsult + ", " + sidsUser + ", " + sidsAdvUser)

        // clear sid from consult role
        List<String> sidToUnassign = new ArrayList<>();
        for( String sid : sidsConsult){
            if( !sid.equals(adGroupConsult)) {
                sidToUnassign.add(sid)
            }
        }
        for( String sid : sidToUnassign){
            println("remove user ${sid} from role consult")
            rolesMap.unAssignRole(rolesMap.getRole(trigram + "-consult"), sid)
        }

        // clear sid from user role
        sidToUnassign = new ArrayList<>();
        for( String sid : sidsUser){
            if( !sid.equals(adGroupUser)) {
                sidToUnassign.add(sid)
            }
        }
        for( String sid : sidToUnassign){
            println("remove user ${sid} from role user")
            rolesMap.unAssignRole(rolesMap.getRole(trigram + "-user"), sid)
        }

        rolesMap.clearSidsForRole(rolesMap.getRole(trigram + "-user"))

        // clear sid from advance user role
        //sidToUnassign = new ArrayList<>();
        //for( String sid : sidsAdvUser){
        //    if( !sid.equals(adGroupAdvUser)) {
        //        sidToUnassign.add(sid)
        //    }
        //}
        //for( String sid : sidToUnassign){
        //    println("remove user ${sid} from role advuser")
        //    rolesMap.unAssignRole(rolesMap.getRole(trigram + "-advuser"), sid)
        //}

        Jenkins.get().save()
    }
}

unassignSid("ado")
unassignSid("evp")
unassignSid("evb")
unassignSid("mab")
unassignSid("dvo")



