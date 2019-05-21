package it.polito.dp2.NFFG.sol2;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import it.polito.dp2.NFFG.FactoryConfigurationError;
import it.polito.dp2.NFFG.LinkReader;
import it.polito.dp2.NFFG.NffgReader;
import it.polito.dp2.NFFG.NffgVerifier;
import it.polito.dp2.NFFG.NffgVerifierException;
import it.polito.dp2.NFFG.NffgVerifierFactory;
import it.polito.dp2.NFFG.NodeReader;
import it.polito.dp2.NFFG.lab2.NoGraphException;
import it.polito.dp2.NFFG.lab2.ServiceException;
import it.polito.dp2.NFFG.lab2.UnknownNameException;

public class ReachabilityTester implements it.polito.dp2.NFFG.lab2.ReachabilityTester {
	NffgReader loaded_nffg= null;
	Set<NodeReader> nodes= new HashSet<NodeReader>();
	static WebTarget target;
	static Client client;
	private static NffgVerifier ver;
	static String baseURI= System.getProperty("it.polito.dp2.NFFG.lab2.URL");
	
	public ReachabilityTester(){

		client = ClientBuilder.newClient();	    
		target = client.target(getBaseURI());
		if(System.getProperty("it.polito.dp2.NFFG.lab2.URL")==null){
			System.setProperty("it.polito.dp2.NFFG.lab2.URL", "http://localhost:8080/Neo4JXML/rest");
		}
		//String baseURI=System.getProperty("it.polito.dp2.NFFG.lab2.URL");
		
		System.setProperty("it.polito.dp2.NFFG.NffgVerifierFactory", "it.polito.dp2.NFFG.Random.NffgVerifierFactoryImpl");
		
		try {
			ver= NffgVerifierFactory.newInstance().newNffgVerifier();
		} catch (NffgVerifierException e) {
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			e.printStackTrace();
		}		
	}	
	@Override
	public void loadNFFG(String name) throws UnknownNameException, ServiceException {
		
		//System.out.println("back from delete");
		loaded_nffg=ver.getNffg(name);
		
		if(loaded_nffg!=null){
		nodes = loaded_nffg.getNodes();
		target= client.target(getBaseURI());
		deleteAllNodes();
		for(NodeReader nr:nodes){
			try {
				Node node= new Node();	
				
				Property prop= new Property();
				prop.setName("name");
				prop.setValue(nr.getName());
				node.getProperty().add(prop);	//set name property				

				performNodePost(node);
				for(LinkReader lr:nr.getLinks()){			
					Relationship rship= new Relationship();
					//SOURCE NODE
					Node srcnode= new Node();				
					Property sprop= new Property();
					sprop.setName("name");
					sprop.setValue(lr.getSourceNode().getName());
					srcnode.getProperty().add(sprop);	//set property name	
	
					rship.setSrcNode(Integer.toString(nodeExists(srcnode)));
					
					//DESTINATION NODE
					Node dstnode=new Node();		
					Property dprop= new Property();
					dprop.setName("name");
					dprop.setValue(lr.getDestinationNode().getName());
					dstnode.getProperty().add(dprop);	//set property name	
						
					rship.setDstNode(Integer.toString(nodeExists(dstnode)));
					
					rship.setType("Link");					
					performRelationPost(rship);
				}					
			}
			catch (FactoryConfigurationError e) {
				e.printStackTrace();
			}
		}
	}//end of if
		else{
			System.out.println("Unknown nffg");
			UnknownNameException u= new UnknownNameException();
			throw u;
		}
}
	@Override
	public boolean testReachability(String srcName, String destName)
			throws  ServiceException, NoGraphException, UnknownNameException {
		it.polito.dp2.NFFG.lab2.UnknownNameException u= new it.polito.dp2.NFFG.lab2.UnknownNameException();	
		if(srcName==null || destName==null){
			throw u;
		}
			System.out.println("........DONE LOADING NFFG.............");
		System.out.println("......TESTING REACHABILITY........");
		System.out.println("FROM: "+srcName);
		System.out.println("TO: "+destName);
	
		String src = null;
		String dst = null;
	
		Node s= new Node();
		Property sprop= new Property();
		sprop.setName("name");
		sprop.setValue(srcName);
		s.getProperty().add(sprop);
		System.out.println("SOURCE NODE...CHECKING IF IT EXISTS");
		
		for(Node n:getAllNodes()){
			if(s.getProperty().get(0).getValue().equals(n.getProperty().get(0).getValue())){					
				src=n.getId();
				System.out.println("SOURCE NODE EXISTS...WITH ID: "+n.getId());
			}
		}
		System.out.println("out from source node for loop");
		if(src==null){
			throw u;
		}
		Node d= new Node();
		Property dprop= new Property();
		dprop.setName("name");
		dprop.setValue(destName);
		d.getProperty().add(dprop);
		System.out.println("DESTINATION NODE...CHECKING IF IT EXISTS");
		
		for(Node n:getAllNodes()){
				if(d.getProperty().get(0).getValue().equals(n.getProperty().get(0).getValue())){
					dst=n.getId();
					System.out.println("DESTINATION NODE EXISTS...WITH ID: "+n.getId());
				}
		}
		System.out.println("out from destination node for loop");
		if(dst==null){
			throw u;
		}
		
		target = client.target(getBaseURI());			
		System.out.println("SOURCE NODE ID: "+src);
		System.out.println("DESTINATION NODE ID: "+dst);
		List<Path> response= target.path("resource")
									.path("node")
									.path(src)
									.path("paths").queryParam("dst", dst)
									.request()
									.accept(MediaType.APPLICATION_XML)
									.get(new GenericType<List<Path>>() {});
		System.out.println("...RECEIVED PATH GET RESPONSE");
			for(Path p:response){
				if(p.getRelationship().isEmpty()||p.getNode().isEmpty()){
					System.out.println("NOT REACHABLE...about to throw exception..");
					throw u;
				}
				else{return true;}
			
			}
	throw u;
	}
	@Override
	public String getCurrentGraphName()  {
		NoGraphException ng= new NoGraphException();
		if(loaded_nffg!=null){
			return loaded_nffg.getName();}
		else{
			return null;
		}
	}		

	public int performNodePost(Node node){
		System.out.println("--------------- Performing Node Post --------------- \n");
		System.out.println("Node NAME: "+node.getProperty().get(0).getValue());
	
		for(Node n:getAllNodes()){
			if(node.getProperty().get(0).getValue().equals(n.getProperty().get(0).getValue())){
				System.out.println();
				System.out.println("RESPONSE OF EXISTING NODE POST RECEIVED");
				System.out.println("Node ID: "+Integer.parseInt(n.getId()));
				return Integer.parseInt(n.getId());
			}
		}
		
		client = ClientBuilder.newClient();	    
		target = client.target(getBaseURI());
		Node response= target.path("resource")
							 .path("node")
							 .request(MediaType.APPLICATION_XML)
							 .post(Entity.entity(node,MediaType.APPLICATION_XML),Node.class);
		System.out.println();
		System.out.println("RESPONSE OF NEW NODE POST RECEIVED");
		System.out.println("Node ID: "+Integer.parseInt(response.getId()));
		return Integer.parseInt(response.getId());
}
	public void performRelationPost(Relationship rship){
		System.out.println("------------ Performing Relationship Post ------------ \n");
		System.out.println("SOURCE: "+rship.getSrcNode());
		System.out.println("DESTINATION: "+rship.getDstNode());
		System.out.println("TYPE: "+rship.getType());
	    
		target = client.target(getBaseURI());
		
		Relationship response= target.path("resource")
									 .path("node/"+rship.getSrcNode()+"/relationship")
									 .request(MediaType.APPLICATION_XML)									 
									 .post(Entity.entity(rship,MediaType.APPLICATION_XML), Relationship.class);

		System.out.println("RESPONSE OF RELATIONSHIP POST RECEIVED");
		System.out.println("RELATION ID: "+response.getId());
		System.out.println("SOURCE: "+response.getSrcNode());
		System.out.println("DESTINATION: "+response.getDstNode());
		System.out.println("TYPE: "+response.getType());		
	}

	
	public void deleteAllNodes(){    		
			target = client.target(getBaseURI());		
			System.out.println("-----DELETING ALL EXISTING NODES IN THE NEO4J SERVICE----------");
			String response= target.path("resource")
								  .path("nodes")
								  .request(MediaType.APPLICATION_XML)
								  .accept(MediaType.TEXT_PLAIN)
								  .delete(String.class);			
			System.out.println("after delete");
			System.out.println("RESPONSE OF DELETE ALL NODES RECEIVED");
			System.out.println("LIST OF NODES: "+response);
			System.out.println("..........................DONE DELETING..........................");
	}
	public int  nodeExists(Node node){
			Node nresponse;
			for(Node n:getAllNodes()){
				if(node.getProperty().get(0).getValue().equals(n.getProperty().get(0).getValue())){
					return Integer.parseInt(n.getId());
				}
			}
			//if node doesn't exist create the node and return it's id
			System.out.println("NEW NODE IS BEING CREATED...");
			client = ClientBuilder.newClient();	    
			target = client.target(getBaseURI());
			nresponse= target.path("resource")
							 .path("node")
							 .request(MediaType.APPLICATION_XML)
							 .post(Entity.entity(node,MediaType.APPLICATION_XML),Node.class);
			return Integer.parseInt(nresponse.getId());	
						
	}
	public List<Node> getAllNodes(){
		target = client.target(getBaseURI());
		List<Node> response= target.path("resource")
							 .path("nodes")
							 .request()
							 .accept(MediaType.APPLICATION_XML)
							 .get(new GenericType<List<Node>>() {});
		//System.out.println("Node 0: "+response.get(0).getId());
		return response;
	}
	private static URI getBaseURI() {
		if(System.getProperty("it.polito.dp2.NFFG.lab2.URL")==null){
			System.setProperty("it.polito.dp2.NFFG.lab2.URL", "http://localhost:8080/Neo4JXML/rest");
		}
	    return UriBuilder.fromUri(System.getProperty("it.polito.dp2.NFFG.lab2.URL")).build();
	}
	
	public static void main(String[] args) throws UnknownNameException, ServiceException {
		ReachabilityTester t= new ReachabilityTester();	
		client = ClientBuilder.newClient();	    
		target = client.target(getBaseURI());	
		ReachabilityTester rt= new ReachabilityTester();
			rt.deleteAllNodes();
		
	}

}
