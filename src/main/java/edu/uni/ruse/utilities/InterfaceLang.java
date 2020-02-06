package edu.uni.ruse.utilities;

import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Enumeration holding different interface languages.
 * 
 * @author Alexander Andreev
 */
public enum InterfaceLang {
	BG("BulgariaFlag.png"), EN("GreatBritainFlag.png");
	private String langIconPath;

	/**
	 * Constructor, holding the flag image of the specified language.
	 * 
	 * @param logoPath
	 */
	InterfaceLang(String logoPath) {
		langIconPath = logoPath;
	}

	public Image getFlag() throws IOException {
		return ImageIO.read(ClassLoader.getSystemResource(langIconPath));
	}

}
