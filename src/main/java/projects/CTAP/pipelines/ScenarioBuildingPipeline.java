package projects.CTAP.pipelines;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import config.Config;
import controller.Controller;
import core.graph.NodeGeoI;
import core.graph.Activity.ActivityNode;
import core.graph.air.AirNode;
import core.graph.facility.osm.FacilityNode;
import core.graph.geo.CityNode;
import core.graph.population.StdAgentNodeImpl;
import core.graph.rail.gtfs.GTFS;
import core.graph.rail.gtfs.RailNode;
import core.graph.road.osm.RoadNode;
import picocli.CommandLine;
import projects.CTAP.attractiveness.normalized.DefaultAttractivenessModelImpl;
import projects.CTAP.attractiveness.normalized.DefaultAttractivenessModelVarImpl;
import projects.CTAP.graphElements.ActivityCityLink;
import projects.CTAP.transport.DefaultCTAPTransportLinkFactory;
import projects.CTAP.transport.TransitCTAPTransportLinkFactory1;

public class ScenarioBuildingPipeline implements Callable<Integer> {
	
	@CommandLine.Command(
			name = "JTAP",
			description = "",
			showDefaultValues = true,
			mixinStandardHelpOptions = true
	)
	
	@CommandLine.Option(names = {"--configFile","-cf"}, description = "The .xml file containing the configurations")
	private Path configFile;
	
	@CommandLine.Option(names = "--threads", defaultValue = "4", description = "Number of threads to use concurrently")
	private int threads;
	
	private static final Logger log = LogManager.getLogger(ScenarioBuildingPipeline.class);

	public static void main(String[] args) {
		System.exit(new CommandLine(new ScenarioBuildingPipeline()).execute(args));
	}

	@Override
	public Integer call() throws Exception {
		
		Config config = Config.of(configFile.toFile()); 
		Controller controller = new Controller(config);
		controller.run();
		controller.emptyTempDirectory();
		
		//Road------------------------------------------------------------------
		//core.graph.road.osm.Utils.setOSMRoadNetworkIntoNeo4j();
		
		
		//insert GTFS-----------------------------------------------------------
		GTFS gtfs = controller.getInjector().getInstance(GTFS.class);
		core.graph.rail.Utils.deleteRailGTFS();
		core.graph.rail.Utils.insertRailGTFSintoNeo4J(gtfs,"2021-07-18");
		
		//insert air network
		core.graph.air.Utils.insertAirNetworkNeo4j();
		
		//insert cities---------------------------------------------------------
		core.graph.geo.Utils.insertCitiesIntoNeo4JFromCsv(CityNode.class);
	
		//create FacilityNodes from osm-----------------------------------------
		core.graph.facility.osm.Utils.facilitiesIntoNeo4j(config);
		
		//connect FacilityNodes with Cities-------------------------------------
		Map<Class<? extends NodeGeoI>,String> facilityConnMap = new HashMap<>();
		facilityConnMap.put(CityNode.class,"city_id");
		core.graph.Utils.setShortestDistCrossLinkCityFacility(FacilityNode.class,"node_osm_id",facilityConnMap,3);
	
		//create the CityFacStatNodes-------------------------------------------
		core.graph.geo.Utils.addCityFacStatNodeWithHistorical();
		
		//Connections between AIR Network and RAIL Network----------------
		Map<Class<? extends NodeGeoI>,String> airRailConnMap = new HashMap<>();
		airRailConnMap.put(RailNode.class,"stop_id");
		core.graph.Utils.setShortestDistCrossLinkAirRail(AirNode.class,"airport_id",airRailConnMap,3);
		
		//TODO Should we connect them to cities? In the spain model, metro is not included, so in Spain the cities are directly connected to the cities
		//perhaps similar for France? Because of metro? Because in France the regional trains are included in the rail network. 
		//Idea: make sure cross link from city to airport, reflects the kinds of travel times/transfers, etc. that the city transport network has
		
		//Connections between AIR Network and ROAD Network----------------
		Map<Class<? extends NodeGeoI>,String> airRoadConnMap = new HashMap<>();
		airRoadConnMap.put(RoadNode.class,"node_osm_id");
		core.graph.Utils.setShortestDistCrossLinkAirRoad(AirNode.class,"airport_id",airRoadConnMap,3);
		
		//Connections between RoadNetwork and RailNetwork-----------------------
		Map<Class<? extends NodeGeoI>,String> railRoadConnMap = new HashMap<>();
		railRoadConnMap.put(RoadNode.class,"node_osm_id");
		core.graph.Utils.setShortestDistCrossLinkRailRoad(RailNode.class,"stop_id",railRoadConnMap,2);
		
		//Connections between Cities and RoadNetwork----------------
		Map<Class<? extends NodeGeoI>,String> cityRoadConnMap = new HashMap<>();
		cityRoadConnMap.put(RoadNode.class,"node_osm_id");
		core.graph.Utils.setShortestDistCrossLinkCityRoad(CityNode.class,"city_id",cityRoadConnMap,3);
		
		//Connections between Cities and RailNetwork----------------
		Map<Class<? extends NodeGeoI>,String> cityRailConnMap = new HashMap<>();
		cityRailConnMap.put(RailNode.class, "stop_id");
		core.graph.Utils.setShortestDistCrossLinkCityRail(CityNode.class,"city_id",cityRailConnMap,3);
		
		//insert activities-----------------------------------------------------
		core.graph.Activity.Utils.insertActivitiesFromCsv(ActivityNode.class);
		core.graph.Activity.Utils.insertActivitiesLocFromCsv(ActivityCityLink.class);
		
		//insert population-----------------------------------------------------
		core.graph.population.Utils.insertStdPopulationFromCsv(StdAgentNodeImpl.class);
		
		//insert attractiveness-------------------------------------------------
		projects.CTAP.attractiveness.normalized.Utils.insertAttractivenessNormalizedIntoNeo4j(
				(DefaultAttractivenessModelImpl)Controller.getInjector().getInstance(DefaultAttractivenessModelImpl.class),
				new DefaultAttractivenessModelVarImpl());
		
		
		//insert transport links------------------------------------------------
		TransitCTAPTransportLinkFactory1 ctapTranspFactory = new TransitCTAPTransportLinkFactory1();
		ctapTranspFactory.insertCTAPTransportLinkFactory(config.getCtapModelConfig()
				.getTransportConfig().getCtapTransportLinkConfig());
		
		
		//insert destinationProbLinks-------------------------------------------
		projects.CTAP.activityLocationSequence.Utils.insertDestinationProbIntoNeo4j();
		
		return 1;
	}
}
