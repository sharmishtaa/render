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
import org.janelia.alignment.spec.TransformSpec;
import org.janelia.alignment.spec.TransformSpecMetaData;
import org.janelia.alignment.spec.LeafTransformSpec;
import org.janelia.alignment.spec.ReferenceTransformSpec;
import org.janelia.alignment.spec.ListTransformSpec;

import mpicbg.models.PointMatch;
import mpicbg.ij.FeatureTransform;
import mpicbg.models.AbstractModel;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.AffineModel2D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.RigidModel2D;
import mpicbg.models.HomographyModel2D;
import mpicbg.models.NotEnoughDataPointsException;


/**
 * 
 * @author Sharmishtaa Seshamani
 */
public class RegisterTiles2Section
{
	@Parameters
	static private class Params
	{
	  
		@Parameter( names = "--help", description = "Display this note", help = true )
        private final boolean help = false;
                        
        @Parameter( names = "--outputJson", description = "Path to save Json file", required = false )
        public String outputJson = null;
               
        @Parameter( names = "--referencetilespec", description = "Json file containing tile specs", required = true)
        public String referencetilespec = null;
        
        @Parameter( names = "--inputtilespec", description = "Json file containing stitching estimates", required = true)
        public String inputtilespec = null;
        
        @Parameter( names = "--outputtilespec", description = "Json file containing stitching estimates", required = true)
        public String outputtilespec = null;
        
        
        //sift parameters
        @Parameter( names = "--initialSigma", description = "Initial Gaussian blur sigma", required = false )
        private float initialSigma = 1.6f;
        
        @Parameter( names = "--percentSaturated", description = "Percentage of pixels to saturate when normalizing contrast before extracting features", required = false )
        private float percentSaturated = 0.5f;
        
        @Parameter( names = "--steps", description = "Steps per scale octave", required = false )
        private int steps = 3;
        
        @Parameter( names = "--minOctaveSize", description = "Min image size", required = false )
        private int minOctaveSize = 64;
        
        @Parameter( names = "--maxOctaveSize", description = "Max image size", required = false )
        private int maxOctaveSize = 0;
        
        @Parameter( names = "--fdSize", description = "Feature descriptor size", required = false )
        private int fdSize = 8;
        
        @Parameter( names = "--fdBins", description = "Feature descriptor bins", required = false )
        private int fdBins = 8;
        
        //matching parameters
        @Parameter( names = "--modelType", description = "type of model (0=translation,1=rigid,2=similarity,3=affine", required = false )
        public int modelType = 1;
        
        @Parameter( names = "--rod", description = "ROD", required = false )
        public float rod = 0.95f;
        
        @Parameter( names = "--minNumInliers", description = "Minimum number of inliers to output a model/inliers", required = false )
        public int minNumInliers = 10;
        
        @Parameter( names = "--maxEpsilon", description = "Maximum distance to consider a point an inlier after fitting transform", required = false )
        public float maxEpsilon = 2.5f;
        
        @Parameter( names = "--Niters", description = "max number of iterations for ransac", required = false )
        public int Niters = 1000;
        
        @Parameter( names = "--minInlierRatio", description = "minimum ratio of inliers/total features to consider it a fit", required = false )
        public float minInlierRatio = 0.0f;

        @Parameter( names = "--referenceID", description = "referenceID for transform that is computed", required = true )
        public String referenceID = null;

        
	}
	
	private RegisterTiles2Section() {}
	
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
	
	public static ImagePlus createSection(List<TileSpec> tileSpecs, ArrayList<ImagePlus> tiles, ArrayList <InvertibleBoundable> models)
	{
		for (int i = 0; i < tileSpecs.size(); i++)
  		{
  			LeafTransformSpec lts = (LeafTransformSpec)tileSpecs.get(i).getLastTransform();
  			String [] tr_string = lts.getDataString().split(" ");
  			
  			double [] tr = {Double.parseDouble(tr_string[4]),Double.parseDouble(tr_string[5])};
  			TranslationModel2D model = new TranslationModel2D();
  			model.set(tr[0], tr[1]);
  			models.add((InvertibleBoundable)model);
  		}
  		ImagePlus Section = Fusion.fuse(new UnsignedShortType(), tiles, models, 2, true, 0,null, false, false, false);
  		return Section;
	}
	
	public static void extractfeatures(ImagePlus imp, List< Feature > fs, Params params)
	{
		if ( imp == null )
	          System.err.println( "Failed to load image!!!" );
	    else
	        {
	            ImageProcessor ip=imp.getProcessor();
	            
	            //enhance contrast of image
	            Calibration cal = new Calibration(imp);
	            ImageStatistics reference_stats = ImageStatistics.getStatistics(ip, Measurements.MIN_MAX, cal);
	            ContrastEnhancer cenh=new ContrastEnhancer();
	            cenh.setNormalize(true);
	            cenh.stretchHistogram(ip,params.percentSaturated,reference_stats);
	    
	            /* calculate sift features for the image or sub-region */
	            FloatArray2DSIFT.Param siftParam = new FloatArray2DSIFT.Param();
	            siftParam.initialSigma = params.initialSigma;
	            siftParam.steps = params.steps;
	            siftParam.minOctaveSize = params.minOctaveSize;
	            int maxsize=params.maxOctaveSize;
	            if (params.maxOctaveSize==0){
	              maxsize = (int) Math.min(imp.getHeight()/4,imp.getWidth()/4);
	            }            
	            siftParam.maxOctaveSize = maxsize;
	            siftParam.fdSize = params.fdSize;
	            siftParam.fdBins = params.fdBins;
	            FloatArray2DSIFT sift = new FloatArray2DSIFT(siftParam);
	            SIFT ijSIFT = new SIFT(sift);

	            ijSIFT.extractFeatures( ip, fs );
	            System.out.println( "found " + fs.size() + " features in imagefile" );
	        }
		
	}
	
	
	
	public static void main( final String[] args )
	{
		
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
        		jc.setProgramName( "java [-options] -cp something.jar org.janelia.alignment.RegisterSections" );
        		jc.usage(); 
        		return;
        	}
					
  		List<TileSpec> inputtileSpecs = readtilespecs(params.inputtilespec);
  		List<TileSpec> referencetileSpecs = readtilespecs(params.referencetilespec);
  		List<String> inputfiles= readandsetinput(inputtileSpecs);
  		List<String> referencefiles= readandsetinput(referencetileSpecs);
  		
  		ArrayList<ImagePlus> inputtiles = read_images(inputfiles);
  		ArrayList<ImagePlus> referencetiles = read_images(referencefiles);
  		ArrayList <InvertibleBoundable> inputmodels = new ArrayList();
  		ArrayList <InvertibleBoundable> referencemodels = new ArrayList();
  		
  		ImagePlus referenceSection = createSection(referencetileSpecs, referencetiles, referencemodels);
  		final List< Feature > referencefs = new ArrayList< Feature >();
  		extractfeatures(referenceSection,referencefs, params);  		
  		
  		
  		
  		ArrayList<ListTransformSpec> alltransformspecs = new ArrayList();
  		List<TileSpec> outputtileSpecs = inputtileSpecs;
  		ArrayList <InvertibleBoundable> models = new ArrayList();
  		ArrayList<Integer> numinliers = new ArrayList();
  		
  		for (int i = 0; i < inputtiles.size(); i++)
  		{
  			final List< Feature > tilefs = new ArrayList< Feature >();
  			extractfeatures(inputtiles.get(i),tilefs, params);
  			final List< PointMatch > candidates = new ArrayList< PointMatch >();
  			FeatureTransform.matchFeatures( tilefs,referencefs, candidates, params.rod );
  			ArrayList<PointMatch> inliers = new ArrayList< PointMatch >();
  		    AbstractModel< ? > model = new RigidModel2D();
  		    System.out.println("fitting model ..");
  		    boolean modelFound;
  		    try
  		    {
  		    	modelFound = model.filterRansac(
  					candidates,
  					inliers,
  					params.Niters,
  					params.maxEpsilon,
  					params.minInlierRatio,
  					10 );
  		    	System.out.println("model found with " + inliers.size() + " inliers");
  		    }
  		    catch ( final NotEnoughDataPointsException e )
  		    {
  		    	System.out.println("no model found..");
  		    	modelFound = false;
  		    }
  		    
  		    numinliers.add(inliers.size());
  		    
  		    models.add((InvertibleBoundable)model);
  		    String modelstring = model.toString();
  		    System.out.println(model);
  		    modelstring = modelstring.replace("[", "");
  		    modelstring = modelstring.replace("]", "");
  		    modelstring = modelstring.replace("AffineTransform", "");
  		    String[] modelvalues = modelstring.split("[,()]");
  		    String datastring = "";
  		    String classname = "mpicbg.trakem2.transform.AffineModel2D";
  		    datastring = modelvalues[2] + " " + modelvalues[3] + " " + modelvalues[5] + " " + modelvalues[6] + " " + modelvalues[4] + " " + modelvalues[7];
  		
  		    int pos = inputfiles.get(i).lastIndexOf("_F");
  		    String tilereferenceID = params.referenceID + inputfiles.get(i).substring(pos,pos+6);
  		    LeafTransformSpec lts = new LeafTransformSpec(classname, datastring);
  		    System.out.println(datastring);
    		ListTransformSpec mylist = new ListTransformSpec(tilereferenceID,null);
    		mylist.addSpec(lts);
    		alltransformspecs.add(mylist);
    		inputtileSpecs.get(i).setTransforms(mylist);
  		    
  		}
  		
       //output to json 
      	final StringBuilder json = new StringBuilder(16 * 1024);
      	for (int n = 0; n< inputtileSpecs.size(); n++)
      	{
      		if (numinliers.get(n) > 0)
      		{
      			String myjson =inputtileSpecs.get(n).toJson() ;
      			if (n < inputtileSpecs.size()-1)
      				myjson = myjson + ","; 
      			json.append(myjson);
      		}
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
      	
  		ImagePlus outSection = Fusion.fuse(new UnsignedShortType(), inputtiles, models, 2, true, 0,null, false, false, false);
  		FileSaver fs = new FileSaver( outSection );
  		fs.saveAsTiff("fusedimage.tif");
      	
		
	}
  		
  		
}