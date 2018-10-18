package buildscripts

import jenkins.util.*;
import jenkins.model.*;
import hudson.model.*;
import hudson.plugins.release.*;

public class BuildTools_V10 {

    def build;

    public BuildTools_V10(AbstractBuild build){
        this.build = build;
    }
    def getAction() {
        def myList = build.getActions();
        def found = null;
        for (action in myList) {
            if (action instanceof ParametersAction) {
                //listener.logger.println "Found ParametersAction"
                found = action;
                break;
            }
        }
        return found;
    }


    def setBuildParam(ParameterValue param) {
        ParametersAction action = getAction();
        if (action != null) {
            def myList = [];
            myList.addAll(action.getParameters());
            myList.add(param);

            build.removeAction(action);
            build.addAction(action.createUpdated(myList));
        } else {
            build.addAction(new ParametersAction(param));
        }
    }

    def setStringBuildParam(String name, String value) {
        def param = new StringParameterValue(name, value);
        setBuildParam(param);
    }

    def setBooleanBuildParam(String name, Boolean value) {
        def param = new BooleanParameterValue(name, value);
        setBuildParam(param);
    }

}

