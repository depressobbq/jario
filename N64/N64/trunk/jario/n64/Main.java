/**
 * Copyright 2009, 2013 Jason LaDere
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package jario.n64;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

public class Main
{
	public static void main(String[] args)
	{
		try
		{
			File dir = new File("components" + File.separator);
			File file = new File("components.properties");
			ClassLoader loader = Main.class.getClassLoader();
			Properties prop = new Properties();
			try
			{
				if (dir.exists() && dir.listFiles().length > 0)
				{
					File[] files = dir.listFiles();
					URL[] urls = new URL[files.length];
					for (int i = 0; i < files.length; i++) urls[i] = files[i].toURI().toURL();
					loader = new URLClassLoader(urls, Main.class.getClassLoader());
				}
				URL url = file.exists() ? file.toURI().toURL() : loader.getResource("resources" + File.separator + "components.properties");
				if (url != null) prop.load(url.openStream());
			}
			catch (IOException e)
			{
			}

			Class.forName(prop.getProperty("SYSTEM", "SYSTEM"), true, loader).newInstance();
		}
		catch (Exception e)
		{
			System.err.println("Missing resources.");
			e.printStackTrace();
		}
	}
}
