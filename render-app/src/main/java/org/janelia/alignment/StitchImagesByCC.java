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
import java.awt.AlphaComposite;
import java.awt.Graphics;
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

import java.util.HashMap;
import java.util.Map;
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
public class StitchImagesByCC
{
	@Parameters
	static private class Params
	{
	  
    
		@Parameter( names = "--help", description = "Display this note", help = true )
        private final boolean help = false;

        @Parameter( names = "--imageFiles", description = "path to image files", required = false )
        private List<String> files;
        
        @Parameter( names = "--xoffsets", description = "x offsets", required = false )
        private List<Float> xoffsets;
        
         @Parameter( names = "--yoffsets", description = "y offsets", required = false )
        private List<Float> yoffsets;
        
        @Parameter( names = "--outputLayout", description = "path to save the layout file for these frames", required = false )
        private String outputLayout = null;
        
        @Parameter( names = "--outputImage", description = "Path to save image file", required = false )
        public String outputImage = null;
       
        @Parameter( names = "--outputJson", description = "Path to save Json file", required = false )
        public String outputJson = null;
       
        
        @Parameter( names = "--addChannel", description = "Channel to add", required = false )
        public List<String> addChannel = null;
        
        @Parameter( names = "--channelWeights", description = "Weights of channels to add", required = false )
        public List<Float> channelWeights = null;
        
        @Parameter( names = "--inputtilespec", description = "Json file containing tile specs", required = false)
        public String inputtilespec = null;
        
        //test
	}
	
	private StitchImagesByCC() {}
	
	public static ArrayList<ImagePlus> read_images(List<String> filepaths)
	{
	    ArrayList<ImagePlus> images = new ArrayList();
	    
	    for ( final String filepath : filepaths )
	    {
	    	System.out.println( filepath );
	        ImagePlus img = IJ.openImage(filepath);
	        images.add(img);   
	    }
	    return images;
	}
	
	public static void calculatediffs(ArrayList<ImageCollectionElement> elements, ArrayList<ImagePlus> imagesR, ArrayList<ImagePlus> images, Map<String,Integer> connects, Map<String,Integer> filehash, Map<String,Integer> rawelementshash, ArrayList <ImagePlusTimePoint> originaloptimized, ArrayList <ImagePlusTimePoint> optimized,ArrayList <InvertibleBoundable> models, Params params, StitchingParameters stitchparams, ArrayList<Float> rawx, ArrayList<Float> rawy)
	{
		Integer i = 0;
		Integer j = 0;
		Integer r = rawelementshash.get(originaloptimized.get(0).getImagePlus().getTitle()); //reference element
		ImagePlusTimePoint imtref = originaloptimized.get(0);
		TranslationModel2D modref = (TranslationModel2D)imtref.getModel();
		Integer k = 0;
		double[] pt = new double[ 2 ]; pt[0] = 0; pt[1] = 0;
		double[] tr = new double[2]; tr[0] = 0; tr[1] = 0;
		double[] raw = new double[2]; 
    	
		// Calculate diffs for debugging and force images that the optimization throws out to come back
		ArrayList<Double> diffs = new ArrayList(); 
		String csvfile = params.outputImage.replaceAll("tif", "csv");
		System.out.println(csvfile);
		File file = new File(params.outputImage);
      	file.getParentFile().mkdirs();
      	
      	try
		{
			FileWriter f0 = new FileWriter(csvfile);			
    		String newLine = System.getProperty("line.separator");

    		while (i < imagesR.size()) 
    		{
				ImagePlus ip = 	imagesR.get(i);		
				images.add(ip);
				if (connects.containsKey( ip.getTitle() ))
				{
					System.out.println("This file is here:");
					System.out.println(ip.getTitle());

					j = filehash.get(ip.getTitle());
					ImagePlusTimePoint imt = originaloptimized.get(j);
					optimized.add(imt);
	        		models.add((InvertibleBoundable) imt.getModel());
	        		//check for difference
	        		k = rawelementshash.get(ip.getTitle());
              		raw[0] = rawx.get(k) - rawx.get(r); 
              		raw[1] = rawy.get(k) - rawy.get(r);
              		TranslationModel2D mod = (TranslationModel2D)imt.getModel();
              		tr = mod.apply(pt);
              		double diffx = (tr[0]-raw[0])*(tr[0]-raw[0]);
              		double diffy = (tr[1]-raw[1])*(tr[1]-raw[1]);
              		double diff = Math.sqrt(diffx+diffy);
					diffs.add(diff);
					f0.write(ip.getTitle()+","+diff+","+mod.getCost()+"\n");
				}

				else
				{
					System.out.println("This file is not here:");
					System.out.println(ip.getTitle());
					ImageCollectionElement element= elements.get(i); // set it to anything
					k = rawelementshash.get(ip.getTitle());
					final ImagePlusTimePoint imt = new ImagePlusTimePoint( element.open( stitchparams.virtual ), element.getIndex(), 1, element.getModel(), element ); //initialize
					final TranslationModel2D model = (TranslationModel2D)imt.getModel();	// relate the model to the imt
					tr = modref.apply(pt);		
					model.set( rawx.get(k)-rawx.get(r)+tr[0], rawy.get(k) -rawy.get(r)+tr[1]);
					optimized.add(imt);
		        	models.add((InvertibleBoundable) imt.getModel());
					diffs.add(-1.0);
				}
				i++;
    		}//end while
    		f0.close();
		}//end try

      	catch(IOException e)
      	{
      		e.printStackTrace();
      	}
	}
	
	public  static void outputtojson(ArrayList<ImagePlus> imagesR, ArrayList<TranslationModel2D> translation_models, Map<String,Integer> rawelementshash, List<TileSpec> tileSpecs, Params params)
	{
		for (int w = 0; w < imagesR.size(); w++)
        {
      		String fname = imagesR.get(w).getTitle();
      		Integer p = rawelementshash.get(fname);
      		double [] l = {0,0};
      		TranslationModel2D mod = translation_models.get(w); //translation_models.get(w);
      		mod.applyInPlace(l);
      		LeafTransformSpec lts = (LeafTransformSpec)tileSpecs.get(p).getLastTransform();
      		String olddatastring = lts.getDataString();
      		String [] op = lts.getDataString().split(" ");
      		op[4] = Double.toString(l[0]); op[5] = Double.toString(l[1]);
      		String newdatastring = "";
      		for (int q = 0; q < op.length; q++)
      			newdatastring = newdatastring + op[q] + " ";
      		LeafTransformSpec newlts = new LeafTransformSpec(lts.getClassName(), newdatastring);
      		ListTransformSpec mylist = new ListTransformSpec();
      		mylist.addSpec(newlts);
      	
      		//set the tilespec's transform to the new one
      		tileSpecs.get(p).setTransforms(mylist);
      		
        }

      	final StringBuilder json = new StringBuilder(16 * 1024);
      	for (int n = 0; n< tileSpecs.size(); n++)
      	{
      		String myjson =tileSpecs.get(n).toJson() ;
      		if (n < tileSpecs.size()-1)
      			myjson = myjson + ","; 
      		json.append(myjson);
      	}
      	
      	
      	
      	
      	
      	FileOutputStream jsonStream = null;
      	String par1 = "[";
      	String par2 = "]";
      		
      	try 
      	{
      		jsonStream = new FileOutputStream(params.outputJson);
      		jsonStream.write(par1.getBytes());
            jsonStream.write(json.toString().getBytes());
            jsonStream.write(par2.getBytes());
        } 
      	catch (final IOException e) 
      	{
            throw new RuntimeException("failed to write to JSON stream", e);
        }
	}
	
	public static ArrayList<String> readandsetinput(List<TileSpec> tileSpecs, ArrayList<ImageCollectionElement> elements,Map<String,Integer> rawelementshash, ArrayList<Float> rawx, ArrayList<Float> rawy )
	{
		//Input Files
        ArrayList<String> allfiles = new ArrayList<String> ();
        for (int i = 0; i < tileSpecs.size(); i++)
        {
        	String fullfname = tileSpecs.get(i).getFirstMipmapEntry().getValue().getImageUrl().toString();
        	int pos = fullfname.lastIndexOf("/");
        	String filenameonly = fullfname.substring(pos+1);
        	String [] fnames = fullfname.split(":");
            File file = new File(fnames[1]);
            ImageCollectionElement element=new ImageCollectionElement(file,i);
        	element.setDimensionality( 2 );
        	LeafTransformSpec lts = (LeafTransformSpec)tileSpecs.get(i).getLastTransform();
        	String [] sltsparams = lts.getDataString().split(" ");
        	float[] offsets = {Float.parseFloat(sltsparams[4]),Float.parseFloat(sltsparams[5])};        	
        	element.setOffset(offsets);
        	element.setModel(new TranslationModel2D() );
        	elements.add(element);
        	rawx.add(Float.parseFloat(sltsparams[4]));
        	rawy.add(Float.parseFloat(sltsparams[5]));
        	rawelementshash.put(filenameonly,i);
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
        		jc.setProgramName( "java [-options] -cp /home/sharmishtaas/allen_SB_code/render/render-app/target/render-app-0.3.0-SNAPSHOT-jar-with-dependencies.jar org.janelia.alignment.StitchImagesByCC" );
        		jc.usage(); 
        		return;
        	}
				
		
		//Stitching Parameters
		StitchingParameters stitchparams=new StitchingParameters();
  		stitchparams.dimensionality = 2;
  		stitchparams.channel1 = 0;
  		stitchparams.channel2 = 0;
  		stitchparams.timeSelect = 0;
  		stitchparams.checkPeaks = 5;
  		stitchparams.regThreshold = 0.4f;
  		stitchparams.computeOverlap = true;
  		stitchparams.subpixelAccuracy = true;
  		stitchparams.fusionMethod = 2;
  		
  		//raw element storage and initialization
  		System.out.println("test");
  		ArrayList<ImageCollectionElement> elements=new ArrayList();
  		ArrayList<ImageCollectionElement> rawelements = new ArrayList();
  		ArrayList<Float> rawx = new ArrayList();
  		ArrayList<Float> rawy = new ArrayList();
  		Map<String,Integer> rawelementshash = new HashMap<String,Integer> ();
  		List<TileSpec> tileSpecs = readtilespecs(params.inputtilespec);
  		params.files= readandsetinput(tileSpecs, elements,rawelementshash,rawx,rawy);
  		
        //registration
  		ImageCollectionandHashContainer cont =new ImageCollectionandHashContainer(); 
  		cont = CollectionStitchingImgLib.stitchCollectionandcalculateconnections( elements, stitchparams );
  		ArrayList <ImagePlusTimePoint> originaloptimized = cont.getIPTPArray();
  		Map<String,Integer> connects = cont.getconnections();
  		ArrayList <ImagePlusTimePoint> optimized = new ArrayList();
  		Map<String,Integer> filehash = new HashMap<String,Integer> ();
  		
  		
  		
  		//read in the images and create hashes
  		ArrayList<ImagePlus> imagesR = read_images(params.files);
  		ArrayList<ImagePlus> images = new ArrayList<ImagePlus> ();
  		ArrayList <InvertibleBoundable> models = new ArrayList();
  		Integer i = 0, j = 0;
  		while (i < originaloptimized.size()) 
  		{
  			connects.put(originaloptimized.get(i).getImagePlus().getTitle(),i);
  			i++;
  		}
  		i = 0;
  		while (i < imagesR.size()) 
  		{
  			if (connects.containsKey( imagesR.get(i).getTitle() ))
  			{
  				j = connects.get(imagesR.get(i).getTitle());
  				filehash.put(imagesR.get(i).getTitle(), j);
  			}
  			i++;
  		}
  		
  		calculatediffs(elements, imagesR, images, connects, filehash, rawelementshash, originaloptimized, optimized,models,params, stitchparams, rawx, rawy);
  		
  		
      	// add translation models
      	final ArrayList<TranslationModel2D> translation_models = new ArrayList();
      	for (final InvertibleBoundable model : models)
			translation_models.add((TranslationModel2D) model);
		
      	if (params.outputImage != null)
		{
			ImagePlus imp = Fusion.fuse(new UnsignedShortType(), imagesR, models, 2, true, 0,null, false, false, false);
      		FileSaver fs = new FileSaver( imp );
      		fs.saveAsTiff(params.outputImage);
      	}
      	
      	//output to json 
      	outputtojson(imagesR, translation_models, rawelementshash, tileSpecs, params);
      	/*for (int w = 0; w < imagesR.size(); w++)
        {
      		String fname = imagesR.get(w).getTitle();
      		Integer p = rawelementshash.get(fname);
      		double [] l = {0,0};
      		TranslationModel2D mod = translation_models.get(w); //translation_models.get(w);
      		mod.applyInPlace(l);
      		LeafTransformSpec lts = (LeafTransformSpec)tileSpecs.get(p).getLastTransform();
      		String olddatastring = lts.getDataString();
      		String [] op = lts.getDataString().split(" ");
      		op[4] = Double.toString(l[0]); op[5] = Double.toString(l[1]);
      		String newdatastring = "";
      		for (int q = 0; q < op.length; q++)
      			newdatastring = newdatastring + op[q] + " ";
      		LeafTransformSpec newlts = new LeafTransformSpec(lts.getClassName(), newdatastring);
      		ListTransformSpec mylist = new ListTransformSpec();
      		mylist.addSpec(newlts);
      	
      		//set the tilespec's transform to the new one
      		tileSpecs.get(p).setTransforms(mylist);
      		
        }

      	final StringBuilder json = new StringBuilder(16 * 1024);
      	for (int n = 0; n< tileSpecs.size(); n++)
      	{
      		String myjson =tileSpecs.get(n).toJson() ;
      		if (n < tileSpecs.size()-1)
      			myjson = myjson + ","; 
      		json.append(myjson);
      	}
      	
      	FileOutputStream jsonStream = null;
      	String par1 = "[";
      	String par2 = "]";
      		
      	try 
      	{
      		jsonStream = new FileOutputStream(params.outputJson);
      		jsonStream.write(par1.getBytes());
            jsonStream.write(json.toString().getBytes());
            jsonStream.write(par2.getBytes());
        } 
      	catch (final IOException e) 
      	{
            throw new RuntimeException("failed to write to JSON stream", e);
        }
      	
      	*/
        
   	}
}