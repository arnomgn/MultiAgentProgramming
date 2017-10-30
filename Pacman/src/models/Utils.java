package models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class Utils {
	
	/**
	 * Search for Simulater regarding Simulater template
	 * 
	 * @return AID
	 */
	public static AID searchForAgent(Agent agentSource, String agentDescription) {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setName(agentDescription);
		sd.setType(agentDescription);
		template.addServices(sd);
		AID agent = null;
		try {
			DFAgentDescription[] result = DFService.search(agentSource, template);
			int n = result.length;
			
			if (n > 0) {
				agent = result[0].getName();
			}
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		return agent;
	}
	
	/**
	 * Register RootAgent in DF Agent with Constant
	 */
	public static void register(Agent agent, String agentDescription) {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(agent.getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setName(agentDescription);
		sd.setType(agentDescription);
		dfd.addServices(sd);
		try {
			DFService.register(agent, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	
	public static Cell[] convertArrayListToCells(ArrayList list) {
		Cell[] cells = new Cell[9];
		int i;
		
		for(i = 0 ; i < list.size(); i++){
			LinkedHashMap cellMap = (LinkedHashMap)list.get(i);
			cells[i] = new Cell((int)cellMap.get("value"), (int)cellMap.get("nligne"), (int)cellMap.get("ncolonne"));
		}
		return cells;
	}
	
	public static boolean getRandomBoolean() {
	       return Math.random() < 0.5;
	       //I tried another approaches here, still the same result
	   }
	
	public static int randomNumber() {
		Random rand = new Random();
		int random = rand.nextInt(1 - 0 + 1) + 0;
		if (getRandomBoolean()) {
			return random;
		} else {
			return -random;
		}
	}
	
}
