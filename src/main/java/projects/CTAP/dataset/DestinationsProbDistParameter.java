package projects.CTAP.dataset;

import java.util.List;

import core.dataset.ParameterI;

public class DestinationsProbDistParameter implements ParameterI<Long> {

	private double[][] parameter;
	private List<List<Long>>  parameterDescription;
	private String id = "DestinationsProbDistParameter";
	private String description = "City_id - CityDs_id";
	
	public DestinationsProbDistParameter(double[][] parameter,
			List<List<Long>>  parameterDescription) {
		this.parameter = parameter;
		this.parameterDescription = parameterDescription;
	}
	
	public DestinationsProbDistParameter() {}
	
	@Override
	public double[][] getParameter() {
		return this.parameter;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public List<List<Long>> getParameterDescription() {
		return this.parameterDescription;
	}

}

