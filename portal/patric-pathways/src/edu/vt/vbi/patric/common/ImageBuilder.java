/**
 * ****************************************************************************
 * Copyright 2014 Virginia Polytechnic Institute and State University
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */
package edu.vt.vbi.patric.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ImageBuilder {

	int w;

	int h;

	BufferedImage image;

	Graphics2D g2d;


	String SERVER_HOME_DIR;

	final String MAP_FILE_ROOT = "/deploy/jboss-web.deployer/ROOT.war/patric/images/pathways";

	private static final Logger LOGGER = LoggerFactory.getLogger(ImageBuilder.class);

	public ImageBuilder(String map_id) {

		try {
			// reading Server Home Dir
			java.util.List<MBeanServer> list = MBeanServerFactory.findMBeanServer(null);
			MBeanServer server = list.get(0);
			ObjectName objectName = new ObjectName("jboss.system:type=ServerConfig");
			SERVER_HOME_DIR = ((File) server.getAttribute(objectName, "ServerHomeDir")).getAbsolutePath();

//			LOGGER.debug("path: {}", SERVER_HOME_DIR + MAP_FILE_ROOT );

			image = ImageIO.read(new File(SERVER_HOME_DIR + MAP_FILE_ROOT + "/map" + map_id + ".png"));
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		this.w = image.getWidth();
		this.h = image.getHeight();
	}

	public void drawonImage(String type, String text, int left, int top, int height, int width, String color) {

		g2d = image.createGraphics();

		String[] colors = color.split(",");
		g2d.setColor(new Color(Float.parseFloat(colors[0].trim()) / 255, Float.parseFloat(colors[1].trim()) / 255,
				Float.parseFloat(colors[2].trim()) / 255));

		if (type.equals("fill")) {
			g2d.fillRect(left, top, width, height);
		}
		else if (type.equals("text")) {
			g2d.setFont(new Font("Arial", Font.PLAIN, 9));
			g2d.drawString(text, left, top + 8);
		}

	}

	public byte[] getByteArray() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		byte[] bytesOut = baos.toByteArray();
		baos.close();
		return bytesOut;
	}

}
