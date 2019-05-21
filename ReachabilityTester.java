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
		System.setProperty("it.polito.dp2.NFFG.NffgVerifierFactory", "it.polito.dp2.NFFG.Random.NffgVerifierFactoryImpl");		
		try {
			ver= NffgVerifierFactory.newInstance().newNffgVerifier();
		} catch (NffgVerifierException e) {
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			e.printStackTrace(); }
		  catch(Exception e){
			  e.printStackTrace(); }
	}	
	@Override
	public void loadNFFG(String name) throws UnknownNameException, ServiceException {
			
			loaded_nffg=ver.getNffg(name);	
			//Check if NFFG is not null
			if(loaded_nffg!=null){
				nodes = loaded_nffg.getNodes();
				deleteAllNodes();	//delete all nodes in the graph
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
			
							rship.setSrcNode(nodeExists(srcnode));
							
							//DESTINATION NODE
							Node dstnode=new Node();		
							Property dprop= new Property();
							dprop.setName("name");
							dprop.setValue(lr.getDestinationNode().getName());
							dstnode.getProperty().add(dprop);	//set property name	
								
							rship.setDstNode(nodeExists(dstnode));
							
							rship.setType("Link");					
							performRelationPost(rship);
						}
					}
					catch (FactoryConfigurationError e) {
						e.printStackTrace();
					} catch(Exception e){
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
		UnknownNameException u= new UnknownNameException();	
		String src = null;
		String dst = null;
		try {
			if(srcName==null || destName==null){
				throw u; }
			System.out.println("........DONE LOADING NFFG.............");
			System.out.println("......TESTING REACHABILITY........");
			System.out.println("FROM: "+srcName);
			System.out.println("TO: "+destName);		
			//SOURCE NODE
			Node s= new Node();
			Property sprop= new Property();
			sprop.setName("name");
			sprop.setValue(srcName);
			s.getProperty().add(sprop);
			System.out.println("SOURCE NODE...CHECKING IF IT EXISTS");
			//Check if source node already exists in the graph
			for(Node n:getAllNodes()){
				if(s.getProperty().get(0).getValue().equals(n.getProperty().get(0).getValue())){					
					src=n.getId();	//source node exists, get id
					System.out.println("SOURCE NODE EXISTS...WITH ID: "+n.getId());
				}
			}
			if(src==null){
				//Source node doesn't exist, throw UnknownNameException
				throw u;
			}
			Node d= new Node();
			Property dprop= new Property();
			dprop.setName("name");
			dprop.setValue(destName);
			d.getProperty().add(dprop);
			System.out.println("DESTINATION NODE...CHECKING IF IT EXISTS");
			//Check if destination node already exists in the graph
			for(Node n:getAllNodes()){
					if(d.getProperty().get(0).getValue().equals(n.getProperty().get(0).getValue())){
						dst=n.getId();
						System.out.println("DESTINATION NODE EXISTS...WITH ID: "+n.getId());
					}
			}
			if(dst==null){
				//Destination node doesn't exist, throw UnknownNameException
				throw u;
			}					
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
				if(p.getRelationship().isEmpty()&&p.getNode().isEmpty()){
					System.out.println("NOT REACHABLE...about to throw UnknownNameException..");
					throw u;
				}
				else{
					//REACHABLE
					System.out.println("REACHABLE...");
					return true;
				}				
			}
		} catch(Exception e){
			throw e;
		}
	throw u;
	}
	@Override
	public String getCurrentGraphName() {
		if(loaded_nffg!=null){
			return loaded_nffg.getName();}
		else{
			return null;
		}
	}		

	public int performNodePost(Node node){
		Node response = null;
		try {
			System.out.println("--------------- Performing Node Post --------------- \n");
			System.out.println("Node NAME: "+node.getProperty().get(0).getValue());
			//CHECK IF NODE ALREADY EXISTS IN THE GRAPH
			for(Node n:getAllNodes()){
				if(node.getProperty().get(0).getValue().equals(n.getProperty().get(0).getValue())){
					System.out.println();
					System.out.println("RESPONSE OF EXISTING NODE POST RECEIVED");
					System.out.println("Node ID: "+Integer.parseInt(n.getId()));
					return Integer.parseInt(n.getId());	
				}
			}
			//CREATE THE NODE 
			client = ClientBuilder.newClient();	    
			target = client.target(getBaseURI());
			response= target.path("resource")
								 .path("node")
								 .request(MediaType.APPLICATION_XML)
								 .post(Entity.entity(node,MediaType.APPLICATION_XML),Node.class);
			System.out.println();
			System.out.println("RESPONSE OF NEW NODE POST RECEIVED");
			System.out.println("Node ID: "+Integer.parseInt(response.getId()));			
		} catch(Exception e){
			e.printStackTrace();
		}
	return Integer.parseInt(response.getId());
}
	public void performRelationPost(Relationship rship){
		try {
			System.out.println("------------ Performing Relationship Post ------------ \n");
			System.out.println("SOURCE: "+rship.getSrcNode());
			System.out.println("DESTINATION: "+rship.getDstNode());
			System.out.println("TYPE: "+rship.getType());
	
			Relationship response= target.path("resource")
										 .path("node/"+rship.getSrcNode()+"/relationship")
										 .request(MediaType.APPLICATION_XML)									 
										 .post(Entity.entity(rship,MediaType.APPLICATION_XML), Relationship.class);
	
			System.out.println("RESPONSE OF RELATIONSHIP POST RECEIVED");
			System.out.println("RELATION ID: "+response.getId());
			System.out.println("SOURCE: "+response.getSrcNode());
			System.out.println("DESTINATION: "+response.getDstNode());
			System.out.println("TYPE: "+response.getType());	
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	
	public void deleteAllNodes(){  
		try {		
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
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	public String  nodeExists(Node node){
		Node nresponse = null;
		try {		
			for(Node n:getAllNodes()){
				if(node.getProperty().get(0).getValue().equals(n.getProperty().get(0).getValue())){
					return n.getId();
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
				
		} catch(Exception e){
			e.printStackTrace();
		}
		return nresponse.getId();
						
	}
	public List<Node> getAllNodes(){
		List<Node> response=null;
		try {
			target = client.target(getBaseURI());
			response= target.path("resource")
								 .path("nodes")
								 .request()
								 .accept(MediaType.APPLICATION_XML)
								 .get(new GenericType<List<Node>>() {});
		
		} catch(Exception e){
			e.printStackTrace();
		}
		return response;
	}
	private static URI getBaseURI() {
		try {
		if(System.getProperty("it.polito.dp2.NFFG.lab2.URL")==null){
			System.setProperty("it.polito.dp2.NFFG.lab2.URL", "http://localhost:8080/Neo4JXML/rest");
		}
		} catch(Exception e){
			e.printStackTrace();}
	    return UriBuilder.fromUri(baseURI).build();
	}
	
	public static void main(String[] args) throws UnknownNameException, ServiceException {
		try{
			ReachabilityTester t= new ReachabilityTester();	
			client = ClientBuilder.newClient();	    
			target = client.target(getBaseURI());	
			ReachabilityTester rt= new ReachabilityTester();
			rt.deleteAllNodes();
		} catch(Exception e){
			e.printStackTrace();
		}
		
	}

}
