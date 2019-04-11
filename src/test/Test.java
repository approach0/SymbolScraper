/******************************************************************************
* Test.java
*
* Copyright (c) 2018, 2019
* Ritvik Joshi, Parag Mali, Puneeth Kukkadapu, Mahshad Mahdavi, and 
* Richard Zanibbi
*
* Document and Pattern Recognition Laboratory
* Rochester Institute of Technology, USA
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************************/

package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.pdfbox.pdmodel.PDDocument;

import annotations.Annotate;
import annotations.AnnotationReader;
import annotations.data.Annotations;

public class Test {

	public static void testGTDB2() throws FileNotFoundException, IOException {
		
		String path = "/Users/parag/Workspace/GTDB-Dataset/GTDB-2/";

		// done
//		test(path, "Alford94");
//		test(path, "Borcherds86");
//		test(path, "Burstall77");
//		test(path, "Emden76");
//		test(path, "Lusztig89");
//		test(path, "Bergweiler83");
//		test(path, "Cline88");
//		test(path, "Katz99");
//		test(path, "Kontsevich94");
//		test(path, "Lorentz48");
//		test(path, "Brezis83");
//		test(path, "Erbe94");
//		test(path, "Kazhdan79");
//		test(path, "jones83");		
//		test(path, "Li75");
//		test(path, "Gidas79");

	}
	
	
	public static void testGTDB1() throws FileNotFoundException, IOException {
		
		String path = "/Users/parag/Workspace/GTDB-Dataset/GTDB-1/";
		
		// done 
		test(path, "MA_1970_26_38");
//		test(path, "MA_1977_275_292");
//		test(path, "BSMF_1970_165_192");
//		test(path, "AIF_1970_493_498");
//		test(path, "ActaM_1970_37_63");
//		test(path, "Arkiv_1997_185_199");
		test(path, "BSMF_1998_245_271");
		test(path, "MA_1999_175_196");
//		test(path, "AIF_1999_375_404");
//		test(path, "ActaM_1998_283_305");		
//		test(path, "InvM_1970_121_134");
//		test(path, "ASENS_1970_273_284");
//		test(path, "AnnM_1970_550_569");		
//		test(path, "BAMS_1998_123_143");
//		test(path, "ASENS_1997_367_384");
//		test(path, "Arkiv_1971_141_163");		
//		test(path, "InvM_1999_163_181"); 
//		test(path, "KJM_1999_17_36");
//		test(path, "JMKU_1971_373_375");
//		test(path, "JMKU_1971_377_379");
//		test(path, "JMKU_1971_181_194");
//		test(path, "BAMS_1971_1974_1");
//		test(path, "BAMS_1971_1974_2");
//		test(path, "BAMS_1971_1974_3");
//		test(path, "JMS_1975_281_288");
//		test(path, "JMS_1975_289_293");
//		test(path, "JMS_1975_497_506");
//		test(path, "TMJ_1973_317_331");
//		test(path, "TMJ_1973_333_338");
//		test(path, "TMJ_1990_163_193");
//				
	}
	
	
	public static void test(String path, String filename) throws FileNotFoundException, IOException {

		Path pdfPath = Paths.get(path, filename + ".pdf");
		Path csvPath = Paths.get(path, filename + ".csv");
		Path outPath = Paths.get(path, filename + "_out_" + ".pdf");
		Path transformationsPath = Paths.get(path, filename + ".md");

		AnnotationReader reader = new AnnotationReader();		
		Annotations annotations = reader.read(csvPath.toString());
		Annotate visualizations = new Annotate();
		
		Path mathBBPath = Paths.get(path, filename + ".math");
		BufferedWriter mathWriter = new BufferedWriter(new FileWriter(mathBBPath.toString()));
		
		Path charBBPath = Paths.get(path, filename + ".char");
		BufferedWriter charWriter = new BufferedWriter(new FileWriter(charBBPath.toString()));
		
		//writeMathBBToFile(annotations, path, filename);
		
		//Load File
		File file = new File(pdfPath.toString());		
        FileInputStream inpStream = new FileInputStream(file);
        PDDocument document = PDDocument.load(inpStream);
        
        BufferedReader transformationsReader = null;
        
        boolean useTransforms = true;
        
        try {
        	transformationsReader = new BufferedReader(new FileReader(transformationsPath.toString())); 
        } catch(IOException e) {
        	useTransforms = false;
        }
        
        String outputFile = outPath.toString(); 

        visualizations.drawBoundingBoxForImage(
			document, outputFile, annotations, transformationsReader, 
			useTransforms, mathWriter, charWriter);
	}
		
	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		testGTDB1();
		testGTDB2();
	}
	
}
