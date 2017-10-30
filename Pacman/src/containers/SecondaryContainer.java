package containers;

import models.Constants;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class SecondaryContainer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		startWithProfile();
	}
	
	public static void startWithProfile() {
	 
		Runtime rt = Runtime.instance();
		ProfileImpl p = null;
		ContainerController cc;
		AgentController ac;
		int i;
	  	  try{
	  		/**
	  		 * host : null value means use the default (i.e. Localhost)
             * port - is the port number. A negative value should be used for using the default port number.
             * platformID - is the symbolic name of the platform, 
             * isMain : boolean
	  		 */
	  		p  =  new  ProfileImpl(null,-1,"tdia04",false);
	  		
			cc = rt.createAgentContainer(p);
			
			// generate 1 Engine Agent 
			ac = cc.createNewAgent(Constants.ENGINE_DESCRIPTION, "agents.Engine", null);
			ac.start();
						
			
			// generate 1 Environment Agent
			ac = cc.createNewAgent(Constants.ENVIRONMENT_DESCRIPTION, "agents.Environment", null);
			ac.start();
			
			
			// generate MONSTER_NUMBER different Monster Agents
			for(i = 0 ; i < Constants.MONSTER_NUMBER ; i++) {
				int num = (i+1);
				// make each monster unique Monster_i
				String monsterName = Constants.MONSTER_DESCRIPTION + "_" + String.valueOf(num);
				ac = cc.createNewAgent(monsterName, "agents.Monster", new Object[]{num});
				ac.start();
			}
			
			//Generate at the same time an analyzer taking care of a monster 
			for(i = 0 ; i < Constants.MONSTER_NUMBER ; i++) {
				int num = (i+1);
			
				String analyserName= Constants.ANALYZER_DESCRIPTION+"_"+String.valueOf(num);
				ac = cc.createNewAgent(analyserName, "agents.AnalyserAgent",new Object[]{num});
				ac.start();
			}
		
			// generate 1 AI Agent
			ac = cc.createNewAgent(Constants.AI_DESCRIPTION, "agents.ArtificialIntelligenceAgent", null);
			ac.start();
			
			// generate 1 TravelerAgent
			ac = cc.createNewAgent(Constants.TRAVELER_DESCRIPTION, "agents.TravelerAgent", null);
			ac.start();
			
	  	  } catch(Exception ex) {
	  		  ex.printStackTrace();
	  	  }
	}
}
