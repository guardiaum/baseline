package com.jms5.baseline.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.jms5.baseline.beans.SentenceTrainingExample;
import com.jms5.baseline.util.CSVUtil;
import com.jms5.baseline.util.Constants;

public class DatasetsUtil {
	
	/**
	 * Constructs training datasets for use when learning classifiers and extractors
	 * 
	 * @throws IOException
	 */
	public void constructsTrainingDataset(List<String[]> trainingExamples, List<String> listSelectedAttributes)
			throws IOException {

		System.out.println("CONSTRUCTING TRAINING DATASETS");
		System.out.println("TRAINING EXAMPLES SIZE: " + trainingExamples.size());

		Path path = Paths.get(Constants.TRAINING_DATASET);

		if (!Files.exists(path)) {
			Files.createDirectories(path.getParent());
			Files.createFile(path);
		}

		try (BufferedWriter writer = Files.newBufferedWriter(path)) {

			for (String[] trainingExample : trainingExamples) {

				String line = "\"" + trainingExample[0] + "\",\"" + trainingExample[1] + "\",\"" + trainingExample[2]
						+ "\",\"" + trainingExample[3] + "\"\n";
				writer.write(line);
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Converts passed dataset to a list SentenceTrainingExample 
	 * 
	 * @param datasetPath
	 * @return
	 * @throws IOException
	 */
	public List<SentenceTrainingExample> getTrainingExamples(Path datasetPath) throws IOException {

		List<SentenceTrainingExample> trainingExamples = new ArrayList<SentenceTrainingExample>();

		BufferedReader reader = Files.newBufferedReader(datasetPath);

		String line = reader.readLine();
		while (line != null) {
			List<String> elements = CSVUtil.readLine(line);

			if (elements != null & elements.size() == 4 & elements.get(0) != null & elements.get(1) != null
					& elements.get(2) != null & elements.get(0).equals("country")) {

				trainingExamples.add(new SentenceTrainingExample(elements.get(0), elements.get(1), elements.get(2),
						elements.get(3)));

			}

			line = reader.readLine();
		}

		return trainingExamples;
	}
	
}
