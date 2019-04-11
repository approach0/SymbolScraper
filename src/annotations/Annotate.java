/******************************************************************************
* Annotate.java
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

/*
 * @Author: Parag Mali
 * 
 * This file annotates the pdf files from the following 
 * datatset given the ground truth:
 * 
 * https://github.com/uchidalab/GTDB-Dataset 
 * 
 * It can be used to visualize the ground truth annotations
 */

package annotations;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import annotations.data.Annotations;
import annotations.data.BoundingBox;
import annotations.data.CharData;
import annotations.data.Image;
import annotations.data.Line;
import annotations.data.MathData;
import annotations.data.Sheet;
import annotations.data.Text;
import annotations.data.TextMode;
import constants.Constants;

public class Annotate {

	public void drawBoundingBoxForImage(
		PDDocument document, String ouputFile, 
		Annotations annotations, BufferedReader transformationsReader, boolean useTransforms, 
		BufferedWriter mathWriter, BufferedWriter charWriter
	) throws IOException {

		if(annotations == null || 
			annotations.getSheets() == null ||
			annotations.getSheets().size() < 1) {

			System.out.println("No annotations found!");
			return;
		}

		int sheetCount = 0;

		double[][] transformations = new double[document.getPages().getCount()][4];
		String line;
		int count = 0;

		if(useTransforms) {
			while((line = transformationsReader.readLine()) != null) {

				String[] coords = line.split(",");
				transformations[count][0] = Double.parseDouble(coords[0]);
				transformations[count][1] = Double.parseDouble(coords[1]);
				transformations[count][2] = Double.parseDouble(coords[2]);
				transformations[count][3] = Double.parseDouble(coords[3]);

				count = count + 1;
			}
		}

		PDFRenderer pdfRenderer = new PDFRenderer(document);
		PDDocument annotatedDoc = new PDDocument();

		for(Sheet sheet: annotations.getSheets()) {

			BufferedImage bufferedImage = 
					pdfRenderer.renderImageWithDPI(sheetCount, Constants.SCAN_DPI, ImageType.RGB);

			PDPage page = document.getPage(sheetCount);

			System.out.println("Page id: " + sheetCount + " mediabox: " + page.getMediaBox());

			PDPage annotatedPage = null;
			
			double sx =  (transformations[sheetCount][2]-transformations[sheetCount][0]) / (sheet.getBoundingBox().getRight() - sheet.getBoundingBox().getLeft());
			double sy =  (transformations[sheetCount][3]-transformations[sheetCount][1]) / (sheet.getBoundingBox().getBottom() - sheet.getBoundingBox().getTop());

			double offsetX = transformations[sheetCount][0] - sheet.getBoundingBox().getLeft(); 
			double offsetY = transformations[sheetCount][1] - sheet.getBoundingBox().getTop();

			if(!useTransforms) {
				offsetX = 0;
				offsetY = 0;
				sx = 1;
				sy = 1;
			}

			System.out.println("Offsets:(x, y) --> " + offsetX + " , " + offsetY);

			PDRectangle mediaBox = 
					new PDRectangle(bufferedImage.getMinX(), 
									bufferedImage.getMinY(),
									bufferedImage.getWidth(),
									bufferedImage.getHeight());

			annotatedPage = new PDPage(mediaBox);

			Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
			graphics.setStroke(new BasicStroke(Constants.STROKE_SIZE));

			annotateTextAreas(sheet.getTextAreas(), graphics, transformations[sheetCount][0], transformations[sheetCount][1],
							  useTransforms, charWriter, sheetCount, sx, sy, sheet.getBoundingBox());
			
			annotateImageAreas(sheet.getImageAreas(), graphics, transformations[sheetCount][0], transformations[sheetCount][1],
							   useTransforms, sx, sy, sheet.getBoundingBox());
			
			annotateMathAreas(sheet.getMathAreas(), graphics, transformations[sheetCount][0], transformations[sheetCount][1],
							  useTransforms, mathWriter, sheetCount, sx, sy, sheet.getBoundingBox());
			
			PDImageXObject pdImageXObject = 
					JPEGFactory.createFromImage(annotatedDoc, bufferedImage);

			PDPageContentStream contents = 
					new PDPageContentStream(annotatedDoc, annotatedPage);

			contents.drawImage(pdImageXObject, Constants.X_ORIGIN, Constants.Y_ORIGIN);

			contents.close();

			annotatedDoc.addPage(annotatedPage);
			sheetCount = sheetCount + 1;

		}

		File file = new File(ouputFile);
		annotatedDoc.save(file);
		annotatedDoc.close();
		mathWriter.close();
		charWriter.close();
	}

	private void annotateMathAreas(List<MathData> mathAreas, Graphics2D graphics, double offsetX, 
		double offsetY, boolean useTransforms, BufferedWriter mathWriter, int sheetCount,
		double widthRatio, double heightRatio, BoundingBox sheetBB) throws IOException {

		if(mathAreas == null) {
			return;
		}

		graphics.setColor(Constants.TRASPARENT_YELLOW);

		for(MathData math: mathAreas) {

			BoundingBox boundingBox = math.getBoundingBox();

			if(useTransforms) {
				
				int left = (int)Math.round(widthRatio * (boundingBox.getLeft() - sheetBB.getLeft()) + offsetX);
				int top = (int)Math.round(heightRatio * (boundingBox.getTop() - sheetBB.getTop()) + offsetY);
	
				// left, top, right, bottom
				graphics.drawRect(
					left,
					top,
					(int)Math.round(widthRatio * (boundingBox.getRight() - boundingBox.getLeft())),
					(int)Math.round(heightRatio * (boundingBox.getBottom() - boundingBox.getTop())));
			
				mathWriter.write(sheetCount + "," + 
						left + "," +
						top + "," +
						(int)Math.round(left + widthRatio * (boundingBox.getRight() - boundingBox.getLeft())) + "," +
						(int)Math.round(top + heightRatio * (boundingBox.getBottom() - boundingBox.getTop())) + "\n" );
			
			} else {
				
				// left, top, right, bottom
				graphics.drawRect(
					boundingBox.getLeft().intValue(),
					boundingBox.getTop().intValue(),
					boundingBox.getRight().intValue() - boundingBox.getLeft().intValue(),
					boundingBox.getBottom().intValue() - boundingBox.getTop().intValue());

				mathWriter.write(sheetCount + "," + 
					boundingBox.getLeft().intValue() + "," +
					boundingBox.getTop().intValue() + "," +
					(int)Math.round(boundingBox.getLeft().intValue() + widthRatio * (boundingBox.getRight() - boundingBox.getLeft())) + "," +
					(int)Math.round(boundingBox.getTop().intValue() + heightRatio * (boundingBox.getBottom() - boundingBox.getTop())) + "\n" );
		
			}
			
			fillBoundingBox(graphics, boundingBox, offsetX, offsetY, useTransforms, widthRatio, heightRatio, sheetBB);	
			drawBoundingBox(graphics, boundingBox, offsetX, offsetY, useTransforms, widthRatio, heightRatio, sheetBB);
		}
	}

	private void annotateImageAreas(
			List<Image> imageAreas, Graphics2D graphics, double offsetX, 
			double offsetY, boolean useTransforms,
			double widthRatio, double heightRatio, BoundingBox sheetBB) {

		if(imageAreas == null) {
			return;
		}

		graphics.setColor(Color.MAGENTA);

		for(Image image: imageAreas) {

			BoundingBox boundingBox = image.getBoundingBox();
			drawBoundingBox(graphics, boundingBox, offsetX, offsetY, useTransforms, widthRatio, heightRatio, sheetBB);
		}
	}

	private void annotateTextAreas(
		List<Text> textAreas, Graphics2D graphics, double offsetX, double offsetY, 
		boolean useTransforms, BufferedWriter charWriter, int sheetCount, double widthRatio, 
		double heightRatio, BoundingBox sheetBB) throws IOException {

		if(textAreas == null) {
			return;
		}

		for(Text text: textAreas) {

			BoundingBox boundingBox = text.getBoundingBox();
			annotateLines(text.getLines(), graphics, offsetX, offsetY, useTransforms, charWriter, sheetCount, widthRatio, heightRatio, sheetBB);

			graphics.setColor(Constants.TRASPARENT_RED);
			
			drawBoundingBox(graphics, boundingBox, offsetX, offsetY, useTransforms, widthRatio, heightRatio, sheetBB);
		}
	}

	private void annotateLines(List<Line> lines, Graphics2D graphics, double offsetX, double offsetY, 
			boolean useTransforms, BufferedWriter charWriter, int sheetCount, double widthRatio, 
			double heightRatio, BoundingBox sheetBB) throws IOException {

		if(lines == null) {
			return;
		}

		for(Line line: lines) {

			BoundingBox boundingBox = line.getBoundingBox();
			annotateCharacters(line.getCharacters(), graphics, offsetX, offsetY, useTransforms, charWriter, 
							   sheetCount, widthRatio, heightRatio, sheetBB);

			graphics.setColor(Constants.TRASPARENT_BLUE);
			drawBoundingBox(graphics, boundingBox, offsetX, offsetY, useTransforms, widthRatio, heightRatio, sheetBB);
		}
	}

	private void annotateCharacters(
			List<CharData> characters, Graphics2D graphics, 
			double offsetX, double offsetY, boolean useTransforms, BufferedWriter charWriter, int sheetCount, 
			double widthRatio, double heightRatio, BoundingBox sheetBB) throws IOException {

		if(characters == null) {
			return;
		}

		for(CharData character: characters) {

			BoundingBox boundingBox = character.getBoundingBox();

			if(character.getTextMode() == TextMode.MATH_SYMBOL) {
				graphics.setColor(Constants.TRASPARENT_YELLOW);
			} else {
				graphics.setColor(Constants.TRASPARENT_GREEN);
			}
			
			if(useTransforms) {
				int left = (int)Math.round(widthRatio * (boundingBox.getLeft() - sheetBB.getLeft()) + offsetX);
				int top = (int)Math.round(heightRatio * (boundingBox.getTop() - sheetBB.getTop()) + offsetY);
				
				// Store modified annotations
				charWriter.write(sheetCount + "," + 
						character.getCharacterId() + "," +
						left + "," +
						top + "," +
						(int)Math.round(left + widthRatio * (boundingBox.getRight() - boundingBox.getLeft())) + "," +
						(int)Math.round(top + heightRatio * (boundingBox.getBottom() - boundingBox.getTop())) + "," +
						character.getTextMode() + "," + 
						character.getLinkLabel() + "," +
						character.getParentId() + "," +
						character.getOCRCode() + "\n");
			} else {
				
				// Store modified annotations
				charWriter.write(sheetCount + "," + 
						character.getCharacterId() + "," +
						boundingBox.getLeft() + "," +
						boundingBox.getTop() + "," +
						boundingBox.getRight() + "," +
						boundingBox.getBottom() + "," +
						character.getTextMode() + "," + 
						character.getLinkLabel() + "," +
						character.getParentId() + "," +
						character.getOCRCode() + "\n");
			}

			
			fillBoundingBox(graphics, boundingBox, offsetX, offsetY, useTransforms, widthRatio, heightRatio, sheetBB);			
			drawBoundingBox(graphics, boundingBox, offsetX, offsetY, useTransforms, widthRatio, heightRatio, sheetBB);
		}
	}

	private void fillBoundingBox(Graphics2D graphics, BoundingBox boundingBox, double offsetX, 
			double offsetY, boolean useTransforms, double widthRatio, double heightRatio,
			BoundingBox sheetBB) {

		if(!useTransforms) {
			
			graphics.fillRect(
				boundingBox.getLeft().intValue(),
				boundingBox.getTop().intValue(),
				boundingBox.getRight().intValue() - boundingBox.getLeft().intValue(),
				boundingBox.getBottom().intValue() - boundingBox.getTop().intValue());
		
		} else {
		
			graphics.fillRect(
				(int)Math.round(widthRatio * (boundingBox.getLeft() - sheetBB.getLeft()) + offsetX), 
				(int)Math.round(heightRatio * (boundingBox.getTop() - sheetBB.getTop()) + offsetY),
				(int)Math.round(widthRatio * (boundingBox.getRight() - boundingBox.getLeft())),
				(int)Math.round(heightRatio * (boundingBox.getBottom() - boundingBox.getTop())));
		}
	}

	private void drawBoundingBox(
			Graphics2D graphics, BoundingBox boundingBox, double offsetX, double offsetY,
			boolean useTransforms, double widthRatio, double heightRatio, BoundingBox sheetBB) {

		if(!useTransforms) {
			
			graphics.drawRect(
				boundingBox.getLeft().intValue(),
				boundingBox.getTop().intValue(),
				boundingBox.getRight().intValue() - boundingBox.getLeft().intValue(),
				boundingBox.getBottom().intValue() - boundingBox.getTop().intValue());
		
		} else {
		
			graphics.drawRect(
				(int)Math.round(widthRatio * (boundingBox.getLeft() - sheetBB.getLeft()) + offsetX), 
				(int)Math.round(heightRatio * (boundingBox.getTop() - sheetBB.getTop()) + offsetY),
				(int)Math.round(widthRatio * (boundingBox.getRight() - boundingBox.getLeft())),
				(int)Math.round(heightRatio * (boundingBox.getBottom() - boundingBox.getTop())));
			
		}
	}
}
