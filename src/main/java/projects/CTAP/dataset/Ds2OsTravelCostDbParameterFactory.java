package projects.CTAP.dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import config.Config;
import controller.Controller;
import core.dataset.ParameterFactoryI;
import core.dataset.ParameterI;
import core.dataset.RoutesMap;
import core.graph.LinkI;
import core.graph.NodeGeoI;
import core.graph.cross.CrossLink;
import core.graph.geo.CityNode;
import core.graph.rail.RailLink;
import core.graph.rail.gtfs.RailNode;
import core.graph.road.osm.RoadLink;
import core.graph.road.osm.RoadNode;
import core.graph.routing.RoutingGraph;
import core.graph.routing.RoutingManager;

public class Ds2OsTravelCostDbParameterFactory extends RoutesMap implements ParameterFactoryI {
	
	private final Config config;
	private final RoutingManager rm;
	private final String RAIL_ROAD_GRAPH = "rail-road-graph";
	private final String RAIL_GRAPH = "rail-graph";
	private final String ROAD_GRAPH = "road-graph";
	private final List<Long> citiesOs_ids;
	private final List<Long> citiesDs_ids;
	private Ds2OsTravelCostParameter ds2osParameter;
	
	@Inject
	public Ds2OsTravelCostDbParameterFactory(Config config,RoutingManager rm,List<Long> citiesOs_ids,List<Long> citiesDs_ids) {
		super(config,rm);
		this.config = config;
		this.rm = rm;
		this.citiesOs_ids = citiesOs_ids;
		this.citiesDs_ids = citiesDs_ids;
	}
	
	@Override
	public ParameterI run() {
		
		/*
		 * projections ---------------------------------------------------------
		 */
		//rail-road-graph
		List<Class<? extends NodeGeoI>> nodesRailRoadGraph = new ArrayList<>();
		List<Class<? extends LinkI>> linksRailRoadGraph = new ArrayList<>();
		nodesRailRoadGraph.add(CityNode.class);
		nodesRailRoadGraph.add(RoadNode.class);
		nodesRailRoadGraph.add(RailNode.class);
		linksRailRoadGraph.add(CrossLink.class);
		linksRailRoadGraph.add(RoadLink.class);
		linksRailRoadGraph.add(RailLink.class);
		try {
			this.addProjection(new RoutingGraph(RAIL_ROAD_GRAPH,nodesRailRoadGraph,linksRailRoadGraph,"avg_travel_time"));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//rail-graph
		List<Class<? extends NodeGeoI>> nodesRail = new ArrayList<>();
		List<Class<? extends LinkI>> linksRail = new ArrayList<>();
		nodesRail.add(CityNode.class);
		nodesRail.add(RailNode.class);
		linksRail.add(CrossLink.class);
		linksRail.add(RailLink.class);
		try {
			this.addProjection(new RoutingGraph(RAIL_GRAPH,nodesRail,linksRail,"avg_travel_time"));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
		//road-graph
		List<Class<? extends NodeGeoI>> nodesRoadGraph = new ArrayList<>();
		List<Class<? extends LinkI>> linksRoadGraph = new ArrayList<>();
		nodesRoadGraph.add(CityNode.class);
		nodesRoadGraph.add(RoadNode.class);
		linksRoadGraph.add(CrossLink.class);
		linksRoadGraph.add(RoadLink.class);
		try {
			this.addProjection(new RoutingGraph(ROAD_GRAPH,nodesRoadGraph,linksRoadGraph,"avg_travel_time"));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		/*
		 * SourceRoutesRequest -------------------------------------------------
		 */
		Integer popThreshold = Controller.getConfig().getCtapModelConfig().getPopulationThreshold();
		List<SourceRoutesRequest> os2dsRailRoad = new ArrayList<>();
		List<SourceRoutesRequest> os2dsRail = new ArrayList<>();
		List<SourceRoutesRequest> os2dsRoad = new ArrayList<>();
		
		List<CityNode> locations = null;
		try {
			locations = data.external.neo4j.Utils.importNodes(this.config.getNeo4JConfig().getDatabase(),CityNode.class);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		List<String> citiesDs = locations.stream()
				.filter(e -> e.getPopulation() >= popThreshold).limit(2)    //TODO limit(2) only for test
				.map(CityNode::getCity).collect(Collectors.toList());
		
		List<String> citiesOs = locations.stream()
				.filter(e -> e.getPopulation() < popThreshold) 
				.map(CityNode::getCity).collect(Collectors.toList());
		
		CityNode cityNode = new CityNode();
		citiesDs.forEach(city ->{
			os2dsRailRoad.add(this.new SourceRoutesRequest(RAIL_ROAD_GRAPH,cityNode,"city",city,"city","avg_travel_time",citiesOs));
			os2dsRail.add(this.new SourceRoutesRequest(RAIL_GRAPH,cityNode,"city",city,"city","avg_travel_time",citiesOs));
			os2dsRoad.add(this.new SourceRoutesRequest(ROAD_GRAPH,cityNode,"city",city,"city","avg_travel_time",citiesOs));
		});
		
		
		/*
		 * Collecting routes ---------------------------------------------------
		 */
		try {
			this.addSourceRoutesFromNeo4j(os2dsRailRoad);
			this.addSourceRoutesFromNeo4j(os2dsRail);
			this.addSourceRoutesFromNeo4j(os2dsRoad);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		/*
		 * Parameter array -----------------------------------------------------
		 */
		List<List<String>> parameterDescription = new ArrayList<>();
		List<String> projections = new ArrayList<>();
		projections.add(RAIL_ROAD_GRAPH);
		projections.add(RAIL_GRAPH);
		projections.add(ROAD_GRAPH);
		parameterDescription.add(projections);
		parameterDescription.add(citiesDs);
		parameterDescription.add(citiesOs);
		double[][][] parameter = this.toArray(parameterDescription);
		ds2osParameter = new Ds2OsTravelCostParameter(parameter,parameterDescription);
		
		try {
			this.close();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return this.ds2osParameter;
	}


}