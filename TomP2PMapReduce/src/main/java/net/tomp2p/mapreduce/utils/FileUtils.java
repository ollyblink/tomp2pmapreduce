/* 
 * Copyright 2016 Oliver Zihler 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.tomp2p.mapreduce.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Some handy file methods
 * 
 * @author Oliver
 *
 */
public enum FileUtils {

	INSTANCE;

	public File createTmpFolder(String inputFilePath) {
		File folder = new File(inputFilePath + "/tmp/");
		if (folder.exists()) {
			FileUtils.INSTANCE.deleteTmpFolder(folder);
		}
		folder.mkdirs();
		return folder;
	}

	public File createTmpFolder(String inputFilePath, String tmpFolderName) {
		File folder = new File(inputFilePath + "/" + tmpFolderName + "/");
		if (folder.exists()) {
			FileUtils.INSTANCE.deleteTmpFolder(folder);
		}
		folder.mkdirs();
		return folder;
	}

	public void deleteTmpFolder(File folder) {
		String[] entries = folder.list();
		for (String s : entries) {
			File currentFile = new File(folder.getPath(), s);
			currentFile.delete();
		}
		folder.delete();
	}

	public void getFiles(File f, List<String> pathVisitor) {

		if (f.isFile())
			pathVisitor.add(f.getAbsolutePath());
		else {
			File files[] = f.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					getFiles(files[i], pathVisitor);
				}
			}
		}
	}

	public String readLines(String filePath, Charset charset) {
		String linesAsLine = "";
		ArrayList<String> lines = readLinesFromFile(filePath, charset);
		for (String line : lines) {
			linesAsLine += line + "\n";
		}
		return linesAsLine;
	}

	public ArrayList<String> readLinesFromFile(String filePath, Charset charset) {
		// System.out.println(filePath);
		ArrayList<String> lines = new ArrayList<String>();
		String line = null;

		try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), charset)) {
			while ((line = reader.readLine()) != null) {
				// System.out.println(line);
				lines.add(line);
			}
		} catch (IOException x) {
			System.err.format("IOException:" + line + " %s%n", x);
		}
		return lines;
	}

	public void deleteFilesAndFolder(String outFolder, List<String> pathVisitor) {
		for (String fP : pathVisitor) {
			File file = new File(fP);
			if (file.exists()) {
				// System.err.println("deleting: "+ fP);
				file.delete();
			}
		}
		File file = new File(outFolder);
		if (file.exists()) {
			// System.err.println("deleting: "+ outFolder);
			file.delete();
		}
	}

}