/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.janelia.alignment;

import ij.ImagePlus;

import ij.process.ImageProcessor;
import ij.IJ;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.lang.StringBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.lang.Math;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;


import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2DSIFT;
import mpicbg.ij.SIFT;
import mpicbg.models.CoordinateTransform;
import mpicbg.models.CoordinateTransformList;
import mpicbg.models.InterpolatedCoordinateTransform;
import mpicbg.models.AffineModel2D;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.InvertibleBoundable;

import mpicbg.models.CoordinateTransformMesh;
import mpicbg.ij.TransformMeshMapping;
import ij.process.ImageStatistics;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.plugin.ContrastEnhancer;
import ij.io.FileSaver;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;


import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;


//import mpicbg.imglib.type.numeric.real.FloatType;
//import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
//import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import mpicbg.stitching.fusion.Fusion;
import mpicbg.stitching.ImageCollectionElement;
import mpicbg.stitching.StitchingParameters;
import mpicbg.stitching.ImagePlusTimePoint;
import mpicbg.stitching.CollectionStitchingImgLib;
import mpicbg.stitching.ImageCollectionandHashContainer;

import java.util.*;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import org.janelia.alignment.json.JsonUtils;
import org.janelia.alignment.spec.TileSpec;
import org.janelia.alignment.spec.LeafTransformSpec;
import org.janelia.alignment.spec.ListTransformSpec;


/**
 * 
 * @author Sharmishtaa Seshamani
 */
public class ApplyStitching
{
	@Parameters
	static private class Params
	{
	  
    
		@Parameter( names = "--help", description = "Display this note", help = true )
        private final boolean help = false;
                        
        
        @Parameter( names = "--outputtilespec", description = "Path to save Json file", required = false )
        public String outputtilespec = null;
       
        
        
        @Parameter( names = "--inputtilespec", description = "Json file containing tile specs", required = false)
        public String inputtilespec = null;
        
        @Parameter( names = "--stitchedtilespec", description = "Json file containing stitching estimates", required = false)
        public String stitchedtilespec = null;
        
        //test
	}
	
	private ApplyStitching() {}
	
	
	
	public static ArrayList<String> readandsetinput(List<TileSpec> tileSpecs)
	{
		//Input Files
        ArrayList<String> allfiles = new ArrayList<String> ();
        for (int i = 0; i < tileSpecs.size(); i++)
        {
        	String fullfname = tileSpecs.get(i).getFirstMipmapEntry().getValue().getImageUrl().toString();
        	int pos = fullfname.lastIndexOf("/");
        	String [] fnames = fullfname.split(":");
            allfiles.add(fnames[1]);
        }
        return allfiles;
	}
	
	public static List<TileSpec> readtilespecs(String inputtilespec)
	{
		List<TileSpec> tileSpecs;
		
		//Read in Tile Specs from Json File
		final URI uri = Utils.convertPathOrUriStringToUri(inputtilespec);
		final URL urlObject;
		try 
		{
			urlObject = uri.toURL();
		} 
		catch (final Throwable tt) 
		{
			throw new IllegalArgumentException("failed to convert URI '" + uri + "'", tt);
		}
		InputStream urlStream = null;
		try 
		{
			urlStream = urlObject.openStream();
		} 
		catch (final UnknownHostException uhe) 
		{
			urlStream = null;
		    throw new IllegalArgumentException("Not happening!",uhe);   
		}
		catch (final Throwable t) 
		{
			throw new IllegalArgumentException("failed to load render parameters from " + urlObject, t);
		}
		
		
		final Reader reader = new InputStreamReader(urlStream);
		try 
		{
			tileSpecs = TileSpec.fromJsonArray(reader);
		} 
		catch (final Throwable t) 
		{
			throw new IllegalArgumentException("failed to parse tile specification loaded from this" , t);
		}
		return tileSpecs;
	}
	
	public static int findindex (int i, List<Integer> indexes)
	{
		for (int j = 0; j < indexes.size(); j++)
		{
			if (indexes.get(j) == i)
				return j;
		}
		
		return 0;
	}
	
	
	public static void main( final String[] args )
	{
		
		//ALL PARAMETERS
		final Params params = new Params();
		try
			{
				final JCommander jc = new JCommander( params, args );
		        if ( params.help )
		        {
		        	jc.usage();
		            return;
		   		}
	     	}
       	catch ( final Exception e )
        	{
        		e.printStackTrace();
           		final JCommander jc = new JCommander( params );
        		jc.setProgramName( "java [-options] -cp something.jar org.janelia.alignment.ApplyStitching" );
        		jc.usage(); 
        		return;
        	}
				
		
		
  		//raw element storage and initialization
  		List<TileSpec> inputtileSpecs = readtilespecs(params.inputtilespec);
  		List<TileSpec> stitchedtileSpecs = readtilespecs(params.stitchedtilespec);
  		List<String> files= readandsetinput(inputtileSpecs);
  		List<String> stitchedfiles= readandsetinput(stitchedtileSpecs);
  		List<Integer> framenums =new ArrayList(); 
  		List<Integer> stitchedframenums = new ArrayList();
  		
  		for (int i = 0; i < files.size(); i++)
  		{
  			int pos = files.get(i).lastIndexOf("_F");
  			String sframe = files.get(i).substring(pos+2,pos+6);
  			//System.out.println("files:");
  			//System.out.println(sframe);
  			framenums.add(Integer.parseInt(sframe));
  		}
  		
  		for (int i = 0; i < stitchedfiles.size(); i++)
  		{
  			int pos = stitchedfiles.get(i).lastIndexOf("_F");
  			String sframe = stitchedfiles.get(i).substring(pos+2,pos+6);
  			//System.out.println("stitchedfiles:");
  			//System.out.println(sframe);
  			stitchedframenums.add(Integer.parseInt(sframe));
  		}
        
  		
  		//System.out.println(framenums);
  		//System.out.println(stitchedframenums);
  		
  		
      	for (int i =0; i < inputtileSpecs.size(); i++)
      	{
      		
      			int stitchedindex = findindex(framenums.get(i),stitchedframenums);
      		
      			/*LeafTransformSpec lts = (LeafTransformSpec)stitchedtileSpecs.get(stitchedindex).getLastTransform();
      			LeafTransformSpec newlts = new LeafTransformSpec(lts.getClassName(), lts.getDataString());
      			ListTransformSpec mylist = new ListTransformSpec();
      			mylist.addSpec(newlts);
      			inputtileSpecs.get(i).setTransforms(mylist);*/
      			inputtileSpecs.get(i).setTransforms(stitchedtileSpecs.get(stitchedindex).getTransforms());
      		
      	}
      	
      	//output to json 
      	final StringBuilder json = new StringBuilder(16 * 1024);
      	for (int n = 0; n< inputtileSpecs.size(); n++)
      	{
      		String myjson =inputtileSpecs.get(n).toJson() ;
      		if (n < inputtileSpecs.size()-1)
      			myjson = myjson + ","; 
      		json.append(myjson);
      	}
      	
      	FileOutputStream jsonStream = null;
      	String par1 = "[";
      	String par2 = "]";
      		
      	try 
      	{
      		jsonStream = new FileOutputStream(params.outputtilespec);
      		jsonStream.write(par1.getBytes());
            jsonStream.write(json.toString().getBytes());
            jsonStream.write(par2.getBytes());
        } 
      	catch (final IOException e) 
      	{
            throw new RuntimeException("failed to write to JSON stream", e);
        }
      	
   	}
}