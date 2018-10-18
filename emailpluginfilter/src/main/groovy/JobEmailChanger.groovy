import hudson.plugins.emailext.plugins.EmailTrigger
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider
import hudson.plugins.emailext.plugins.trigger.FailureTrigger
import hudson.plugins.emailext.plugins.trigger.UnstableTrigger
import hudson.plugins.emailext.plugins.RecipientProvider
import jenkins.model.*;
import hudson.plugins.emailext.*
import hudson.model.*
import hudson.util.*
import hudson.tasks.*

def doJobThatSendToCulprits(){

    jobs = Jenkins.getInstance().getAllItems(FreeStyleProject)
    for( job in jobs) {

        //def plugin = jenkins.model.Jenkins.instance.getDescriptorByType(hudson.plugins.emailext.ExtendedEmailPublisherDescriptor.class);
        //plugin.send

        Mailer mailer
        ExtendedEmailPublisher extendedEmailPublisher

        DescribableList<Publisher, Descriptor<Publisher>> publishersList = job.getPublishersList()

        for (Publisher publisher : publishersList) {
            if(publisher instanceof Mailer) {
                mailer = publisher
            } else if(publisher instanceof ExtendedEmailPublisher) {
                extendedEmailPublisher = publisher
            }
        }

        boolean  changed = false
        // check if email plugin is configure to send mail to culprits
        if (mailer?.sendToIndividuals) {
            println("job: " + job.name + ", send to culprits.  recipients: " + mailer.recipients)
            if (extendedEmailPublisher != null) {
                // if recipiant list is empty or by default replace with recipiant list from mailer plugin
                if (extendedEmailPublisher.recipientList.isEmpty() || extendedEmailPublisher.recipientList.equals('$DEFAULT_RECIPIENTS')) {
                    extendedEmailPublisher.recipientList = mailer.recipients
                    changed = true
                } else {
                    // if not null concat with what is already there.
                    extendedEmailPublisher.recipientList = " " + mailer.recipients
                }
                println("job: " + job.name + " changed extMailer recipients list to recipients: " + mailer.recipients)

                // get list of triggers configued for this job
                List<EmailTrigger> triggers = extendedEmailPublisher.getConfiguredTriggers()
                for( trigger in triggers){
                    boolean hasRecipiantList = false
                    // check if trigger for failure or unstable existe
                    if( trigger instanceof FailureTrigger || trigger instanceof UnstableTrigger){
                        List<RecipientProvider> providers = trigger.email.getRecipientProviders()

                        for( RecipientProvider provider : providers) {
                            // if so check if Recipiaant list provider exists
                            if( provider instanceof ListRecipientProvider){
                                hasRecipiantList = true
                                println("job: " + job.name + ", trigger: " + trigger.descriptor.displayName + " has a recipient list provider")
                            }
                        }
                    }
                    if( ! hasRecipiantList) {
                        println("job: " + job.name + ", trigger: " + trigger.descriptor.displayName + " has no recipient list provider")
                    }
                }

                mailer.sendToIndividuals = false
            }
        } else {
            println("job:" + job.name + ' got no mailer configured to send to culprits')
        }

        if(changed){
            job.save()
        }

    }
}

doJobThatSendToCulprits()
