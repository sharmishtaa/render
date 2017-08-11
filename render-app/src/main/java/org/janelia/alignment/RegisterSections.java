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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.Math;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

//import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
//import net.imglib2.type.numeric.integer.UnsignedByteType;

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
import org.janelia.alignment.spec.TileBounds;
import org.janelia.alignment.spec.TileSpec;
import org.janelia.alignment.spec.TransformSpec;
import org.janelia.alignment.spec.TransformSpecMetaData;
import org.janelia.alignment.spec.LeafTransformSpec;
import org.janelia.alignment.spec.ReferenceTransformSpec;
import org.janelia.alignment.spec.ListTransformSpec;
import org.janelia.alignment.spec.ResolvedTileSpecCollection;

import mpicbg.models.PointMatch;
import mpicbg.ij.FeatureTransform;
import mpicbg.models.AbstractModel;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.AffineModel2D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.RigidModel2D;
import mpicbg.models.HomographyModel2D;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.stitching.PairWiseStitchingImgLib;
import mpicbg.stitching.PairWiseStitchingResult;

import ij.gui.Roi;
import java.awt.geom.Rectangle2D;
import java.awt.Rectangle;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileSystems;


import org.janelia.render.client.TilePairClient;
import org.janelia.alignment.match.OrderedCanvasIdPair;
import org.janelia.alignment.spec.TileBounds;
import org.janelia.alignment.spec.TileBoundsRTree;
import org.janelia.render.client.FileUtil;

import mpicbg.imglib.algorithm.fft.PhaseCorrelation;
import mpicbg.imglib.algorithm.fft.PhaseCorrelationPeak;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.scalespace.SubpixelLocalization;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
//import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
//import mpicbg.imglib.type.numeric.RealType;
import mpicbg.stitching.Peak;
import java.lang.*;

/**
 * 
 * @author Sharmishtaa Seshamani
 */
public class RegisterSections 
{
	@Parameters
	static private class Params
	{
	  
		@Parameter( names = "--help", description = "Display this note", help = true )
        private final boolean help = false;
                        
        @Parameter( names = "--outputJson", description = "Path to save Json file", required = true )
        public String outputJson = null;
               
        @Parameter( names = "--referencetilespec", description = "Json file containing tile specs", required = true)
        public String referencetilespec = null;
        
        @Parameter( names = "--inputtilespec", description = "Json file containing stitching estimates", required = true)
        public String inputtilespec = null;
        
        @Parameter( names = "--referencetransformspec", description = "Json file containing tile specs", required = false)
        public String referencetransformspec = null;
        
        @Parameter( names = "--inputtransformspec", description = "Json file containing tile specs", required = false)
        public String inputtransformspec = null;
        
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
        
        @Parameter( names = "--contrastEnhance", description = "Flag for contrast enhancement", required = false )
        private boolean contrastEnhance = true;
        
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
	
	private RegisterSections() {}
	
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
	
	
	public static List<TransformSpec> loadTransformData(final String transformFile) throws IOException 
	{

        final List<TransformSpec> list;

        if (transformFile == null) {

            list = new ArrayList<>();

        } else {

            final Path path = Paths.get(transformFile).toAbsolutePath();

            //LOG.info("loadTransformData: entry, path={}", path);

            try (final Reader reader = FileUtil.DEFAULT_INSTANCE.getExtensionBasedReader(path.toString())) {
                list = TransformSpec.fromJsonArray(reader);
            }
        }

        //LOG.info("loadTransformData: exit, loaded {} transform specs", list.size());

        return list;
    }

    public static List<TileSpec> loadTileData(String tileFile) throws IOException, IllegalArgumentException 
    {
    
        final List<TileSpec> list;

        final Path path = FileSystems.getDefault().getPath(tileFile).toAbsolutePath();

        //LOG.info("loadTileData: entry, path={}", path);

        try (final Reader reader = FileUtil.DEFAULT_INSTANCE.getExtensionBasedReader(path.toString())) {
            list = TileSpec.fromJsonArray(reader);
        }

        //LOG.info("loadTileData: exit, loaded {} tile specs", list.size());

        return list;
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
	
	
	public static double[] gettrans(TileSpec ts)
	{
		LeafTransformSpec lts = (LeafTransformSpec)ts.getLastTransform();
		String [] tr_string = lts.getDataString().split(" ");
		double [] tr = {Double.parseDouble(tr_string[4]),Double.parseDouble(tr_string[5])};
		return tr;
	}
	
	public static ImagePlus createSection(List<TileSpec> tileSpecs, ArrayList<ImagePlus> tiles, ArrayList <InvertibleBoundable> models)
	{
		for (int i = 0; i < tileSpecs.size(); i++)
  		{
  			//LeafTransformSpec lts = (LeafTransformSpec)tileSpecs.get(i).getLastTransform();
			
			//ListTransformSpec l = new ListTransformSpec();
			//tileSpecs.get(0).getTransforms().getSpec(0).resolveReferences(idToSpecMap)
			
			//LeafTransformSpec lts = (LeafTransformSpec) tileSpecs.get(i).getTransforms().getLastSpec();
			//tileSpecs.get(i).getTransforms().getSpec(0).flatten(l);
			
			//LeafTransformSpec lts = (LeafTransformSpec)l.getLastSpec();
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
	
	
	public static ImagePlus createSection(ResolvedTileSpecCollection tileSpecs, List<TileSpec> inputtileSpecs, ArrayList<ImagePlus> tiles, ArrayList <InvertibleBoundable> models)
	{
		
		for (int i = 0; i < inputtileSpecs.size(); i++)
  		{
  			//LeafTransformSpec lts = (LeafTransformSpec)tileSpecs.get(i).getLastTransform();
			
			TileSpec T = tileSpecs.getTileSpec(inputtileSpecs.get(i).getTileId()); 
			T.flattenTransforms();
			//LeafTransformSpec lts = (LeafTransformSpec)T.getLastTransform();
			LeafTransformSpec r = (LeafTransformSpec)T.getLastTransform();
			System.out.println("pritning r before flattening");
			System.out.println(r.getDataString());
			ListTransformSpec flattenedList = new ListTransformSpec();
			r.flatten(flattenedList);
			System.out.println("Printing r to string after flattening");
			System.out.println(r.getDataString());
			//LeafTransformSpec lts = (LeafTransformSpec)l.getLastSpec();
			
			
			LeafTransformSpec lts = (LeafTransformSpec)inputtileSpecs.get(i).getLastTransform();
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
		extractfeatures(imp,fs,params,1);
	}
	
	public static void extractfeatures(ImagePlus imp, List< Feature > fs, Params params, Integer scale)
	{
		if ( imp == null )
	          System.err.println( "Failed to load image!!!" );
	    else
	        {
	            ImageProcessor ip=imp.getProcessor();
	            
	            //scale image
	            if (scale > 1)
	            {
	            	ip.setInterpolationMethod(ImageProcessor.BILINEAR);
	            	ip = ip.resize(imp.getWidth()/scale,imp.getHeight()/scale,true);
	            }
	            
	            
	            
	            //enhance contrast of image
	            Calibration cal = new Calibration(imp);
	            ImageStatistics reference_stats = ImageStatistics.getStatistics(ip, Measurements.MIN_MAX, cal);
	            ContrastEnhancer cenh=new ContrastEnhancer();
	            if (params.contrastEnhance == true)
	            {
					cenh.setNormalize(true);
					cenh.equalize(ip);
					//cenh.stretchHistogram(ip,params.percentSaturated,reference_stats);
				}
	    
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
	
	public static PairWiseStitchingResult computePhaseCorrelation ( final ImagePlus img1, final ImagePlus img2, final int numPeaks, final boolean subpixelAccuracy )
	{
		
		Image <FloatType> image1 = ImagePlusAdapter.wrap(img1);
		Image <FloatType> image2 = ImagePlusAdapter.wrap(img2);
		PhaseCorrelation< FloatType, FloatType > phaseCorr = new PhaseCorrelation<FloatType, FloatType>( image1,image2 );
		phaseCorr.setInvestigateNumPeaks( numPeaks );
		
		
		if ( subpixelAccuracy )
			phaseCorr.setKeepPhaseCorrelationMatrix( true );
		
		phaseCorr.setComputeFFTinParalell( true );
		if ( !phaseCorr.process() )
		{
			System.out.println( "Could not compute phase correlation: " + phaseCorr.getErrorMessage() );
			return null;
		}

		// result
		final PhaseCorrelationPeak pcp = phaseCorr.getShift();
		
		final float[] shift = new float[ img1.getNDimensions() ];
		final PairWiseStitchingResult result;
		
		if ( subpixelAccuracy )
		{
			final Image<FloatType> pcm = phaseCorr.getPhaseCorrelationMatrix();		
		
			final ArrayList<DifferenceOfGaussianPeak<FloatType>> list = new ArrayList<DifferenceOfGaussianPeak<FloatType>>();		
			final Peak p = new Peak( pcp );
			list.add( p );
					
			final SubpixelLocalization<FloatType> spl = new SubpixelLocalization<FloatType>( pcm, list );
			final boolean move[] = new boolean[ pcm.getNumDimensions() ];
			for ( int i = 0; i < pcm.getNumDimensions(); ++i )
				move[ i ] = false;
			spl.setCanMoveOutside( true );
			spl.setAllowedToMoveInDim( move );
			spl.setMaxNumMoves( 0 );
			spl.setAllowMaximaTolerance( false );
			spl.process();
			
			final Peak peak = (Peak)list.get( 0 );
			
			for ( int d = 0; d < img1.getNDimensions(); ++d )
				shift[ d ] = peak.getPCPeak().getPosition()[ d ] + peak.getSubPixelPositionOffset( d );
			
			pcm.close();
			
			result = new PairWiseStitchingResult( shift, pcp.getCrossCorrelationPeak(), p.getValue().get() );
		}
		else
		{
			for ( int d = 0; d < img1.getNDimensions(); ++d )
				shift[ d ] = pcp.getPosition()[ d ];
			
			result = new PairWiseStitchingResult( shift, pcp.getCrossCorrelationPeak(), pcp.getPhaseCorrelationPeak() );
		}
		
		return result;
	}
	
	
	public static RigidModel2D createInverseModel(RigidModel2D initmodel)
	{
		System.out.println("Now calculating inverse.....");
		RigidModel2D model = new RigidModel2D();
		
		//calculate angle
		double [] array = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		initmodel.toArray(array);
		double rot = Math.atan(array[2]/array[3]);
		
		model.set(-1*rot, -1*array[4], -1*array[5]);
		return model;
	}
	
	public static RigidModel2D calculateCC(ImagePlus img1, ImagePlus img2, RigidModel2D initmodel)
	{
		System.out.println("Now calculating CC.....");
		RigidModel2D model = new RigidModel2D();
		float [] offset = {0.0f, 0.0f};
		float maxvalue = 0;
		
		
		double [] array = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		
		initmodel.toArray(array);
		double rot = Math.atan(array[2]/array[3]);
		ImageProcessor ip = img2.getProcessor();
		ip.rotate(rot);
		
		System.out.println(array.toString());
		System.out.println(rot);
		
		PairWiseStitchingResult p = computePhaseCorrelation(img1,img2,5,true);
		offset = p.getOffset();
		
		/*
		 * 	float rot = 0;
		 * for (int i = -5; i < 6; i++)
		{
			System.out.println(i);
			ImageProcessor ip=img1.getProcessor();
			ip.rotate(i*0.1);
			PairWiseStitchingResult p = computePhaseCorrelation(img1,img2,5,true);
			if (p.getCrossCorrelation() > maxvalue)
			{
					offset = p.getOffset();
					//p.crossCorrelation;
					maxvalue = p.getCrossCorrelation();
					rot = i;
					System.out.println(offset);
					System.out.println(maxvalue);
					System.out.println(rot);
					
			}
			
			
		}
		
		System.out.println(offset);
		System.out.println(maxvalue);
		System.out.println(rot);*/
				
		model.set(rot, -1*offset[0], -1*offset[1]);
		return model;
		
		
	}

	
	public static AbstractModel<?> calculatetransform(ResolvedTileSpecCollection resolvedtileSpecsref, ResolvedTileSpecCollection resolvedtileSpecs, List<TileSpec> reftileSpecs, List<TileSpec> inputtileSpecs, Params params)
	{
		
		
		//initialize
		
		AbstractModel<?> model;
		final List< Feature > inputfs = new ArrayList< Feature >();
  		final List< Feature > referencefs = new ArrayList< Feature >();
  		ArrayList <InvertibleBoundable> inputmodels =new ArrayList();
  		ArrayList <InvertibleBoundable> refmodels = new ArrayList();
  		
  		//read tilespecs
  		//ArrayList<String> inputfilenames = readandsetinput(resolvedtileSpecs);
  		
  		//just for compiling
  		ArrayList<String> inputfilenames = readandsetinput(inputtileSpecs);
  		ArrayList<String> reffilenames = readandsetinput(reftileSpecs);
  		ArrayList<ImagePlus> inputimages =  read_images(inputfilenames);
  		ArrayList<ImagePlus> refimages =  read_images(reffilenames);
  		
  		int sc = 1;
  		
  		//extract features
  		
  		ImagePlus inputImage = new ImagePlus();
  		ImagePlus refImage = new ImagePlus();
  		
  		if (inputimages.size() > 1)
  		{
  			//ImagePlus inputSection = createSection(resolvedtileSpecs, inputtileSpecs, inputimages, inputmodels);
  			//System.out.println(inputSection.getType());
  			//extractfeatures(inputSection, inputfs, params);
  			inputImage = createSection(resolvedtileSpecs, inputtileSpecs, inputimages, inputmodels);
  			FileSaver fs = new FileSaver( inputImage );
  			fs.saveAsTiff("inputSection.tif");
  		}
  		else
  		{
  			
  			//extractfeatures(inputimages.get(0), inputfs, params,sc);
  			//System.out.println(inputimages.get(0).getType());
  			//FileSaver fs1 = new FileSaver( inputimages.get(0) );
      		//fs1.saveAsTiff("testinput.tif");
  			inputImage = inputimages.get(0);
  		}
  		
  		extractfeatures(inputImage, inputfs, params);
  		
  		if (refimages.size() > 1)
  		{
  			//ImagePlus refSection = createSection(resolvedtileSpecsref, reftileSpecs, refimages, refmodels);
  			//extractfeatures(refSection, referencefs, params);
  			refImage = createSection(resolvedtileSpecsref, reftileSpecs, refimages, refmodels);
  			FileSaver fs1 = new FileSaver( refImage );
  			fs1.saveAsTiff("refSection.tif");
  		}
  		else
  		{
	  		//extractfeatures(refimages.get(0),referencefs, params,sc);
	  		//FileSaver fs2 = new FileSaver( refimages.get(0) );
      		//fs2.saveAsTiff("testref.tif");
  			refImage = refimages.get(0); 
  		}
  		
  		extractfeatures(refImage,referencefs, params,sc);
	  	
  		
  		//match
  		final List< PointMatch > candidates = new ArrayList< PointMatch >();
		FeatureTransform.matchFeatures( inputfs,referencefs, candidates, params.rod );
		ArrayList<PointMatch> inliers = new ArrayList< PointMatch >();
		switch ( params.modelType )
		{
			case 0:
				model = new TranslationModel2D();
				break;
			case 1:
				model = new RigidModel2D();
				break;
			case 2:
				model = new SimilarityModel2D();
				break;
			case 3:
				model = new AffineModel2D();
				break;
			case 4:
				model = new HomographyModel2D();
				break;
			default:
				model = new RigidModel2D();
				return model;
		}
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
		
		
		return model;
		
	}
	
	public static AbstractModel<?> calculatetransform(List <TileSpec> reftileSpecs, List <TileSpec> inputtileSpecs, Params params, RigidModel2D initmodel)
	{
		
		//initialize
		
		AbstractModel<?> model;
		final List< Feature > inputfs = new ArrayList< Feature >();
  		final List< Feature > referencefs = new ArrayList< Feature >();
  		ArrayList <InvertibleBoundable> inputmodels = new ArrayList();
  		ArrayList <InvertibleBoundable> refmodels = new ArrayList();
  		
  		//read tilespecs
  		ArrayList<String> inputfilenames = readandsetinput(inputtileSpecs);
  		ArrayList<String> reffilenames = readandsetinput(reftileSpecs);
  		ArrayList<ImagePlus> inputimages =  read_images(inputfilenames);
  		ArrayList<ImagePlus> refimages =  read_images(reffilenames);
  		
  		//extract features
  		
  		ImagePlus inputImage = new ImagePlus();
  		ImagePlus refImage = new ImagePlus();
  		
  		if (inputimages.size() > 1)
  		{
  			//ImagePlus inputSection = createSection(inputtileSpecs, inputimages, inputmodels);
  			//System.out.println(inputSection.getType());
  			//extractfeatures(inputSection, inputfs, params);
  			inputImage = createSection(inputtileSpecs, inputimages, inputmodels);
  			FileSaver fs = new FileSaver( inputImage );
  			fs.saveAsTiff("inputSection.tif");
  		}
  		else
  		{
  			//extractfeatures(inputimages.get(0), inputfs, params);
  			//System.out.println(inputimages.get(0).getType());
  			//FileSaver fs1 = new FileSaver( inputimages.get(0) );
      		//fs1.saveAsTiff("testinput.tif");
  			inputImage = inputimages.get(0);
  		}
  		
  		extractfeatures(inputImage, inputfs, params);
  		
  		if (refimages.size() > 1)
  		{
  			//ImagePlus refSection = createSection(reftileSpecs, refimages, refmodels);
  			//extractfeatures(refSection, referencefs, params);
  			refImage = createSection(reftileSpecs, refimages, refmodels);
  			FileSaver fs1 = new FileSaver( refImage );
  			fs1.saveAsTiff("refSection.tif");
  		}
  		else
  		{
	  		//extractfeatures(refimages.get(0),referencefs, params);
	  		//FileSaver fs2 = new FileSaver( refimages.get(0) );
      		//fs2.saveAsTiff("testref.tif");
  			refImage = refimages.get(0);
  			
  		}
  		
  		extractfeatures(refImage, referencefs, params);
  		
  		//match
  		final List< PointMatch > candidates = new ArrayList< PointMatch >();
		FeatureTransform.matchFeatures( inputfs,referencefs, candidates, params.rod );
		ArrayList<PointMatch> inliers = new ArrayList< PointMatch >();
		switch ( params.modelType )
		{
			case 0:
				model = new TranslationModel2D();
				break;
			case 1:
				model = new RigidModel2D();
				break;
			case 2:
				model = new SimilarityModel2D();
				break;
			case 3:
				model = new AffineModel2D();
				break;
			case 4:
				model = new HomographyModel2D();
				break;
			default:
				model = new RigidModel2D();
				return model;
		}
	    boolean modelFound;
		try
		{
			int numinliers = 0;
			int numrounds = 0;
			while (numinliers < 1  & numrounds < 3)
			{
					modelFound = model.filterRansac(
							candidates,
							inliers,
							params.Niters,
							params.maxEpsilon + numrounds*10,
							params.minInlierRatio,
							10 );
					System.out.println("model found with " + inliers.size() + " inliers");
					numinliers = inliers.size();
					numrounds = numrounds + 1;
			}
		}
		catch ( final NotEnoughDataPointsException e )
		{
		  System.out.println("no model found..");
			modelFound = false;
		}
		
		//if (inliers.size() ==0)
		//public static RigidModel2D createInverseModel(RigidModel2D initmodel)
		//model = createInverseModel((RigidModel2D)model);
		if (inliers.size() < 10)
			model = calculateCC(inputImage, refImage, initmodel);
		
		//check what the model has calculated
		double [] initarray = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		initmodel.toArray(initarray);
		RigidModel2D newmodel = new RigidModel2D();
		newmodel = (RigidModel2D)model;
		double [] modelarray = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		newmodel.toArray(modelarray);
		
		System.out.println("Printing model and init model: ");
		System.out.println(model2datastring(model));
		System.out.println(model2datastring(initmodel));
		//commented out by sharmi on may 2 2017, while running on santacruz data
		/*if (Math.abs(-initarray[4]-modelarray[4]) > 100 || Math.abs(-initarray[5]-modelarray[5]) > 100) // if translation is too large, usually caused by dust particles...
		{
			model = initmodel;
		} */
		
		System.out.println(modelarray[4]);
		System.out.println(initarray[4]);
		System.out.println(modelarray[5]);
		System.out.println(initarray[5]);
		System.out.println("final model: ");
		System.out.println(model2datastring(model));
		
		return model;
		
	}
	
	
	
	public static String model2datastring(AbstractModel<?> model)
	{
		String modelstring = model.toString();
		modelstring = modelstring.replace("[", "");
		modelstring = modelstring.replace("]", "");
		modelstring = modelstring.replace("AffineTransform", "");
		String[] modelvalues = modelstring.split("[,()]");
		String datastring = "";
		
		//convert to mpicbg.trakem2.transform.AffineModel2D
		String classname = "mpicbg.trakem2.transform.AffineModel2D";
		datastring = modelvalues[2] + " " + modelvalues[3] + " " + modelvalues[5] + " " + modelvalues[6] + " " + modelvalues[4] + " " + modelvalues[7];
		return datastring;
	}
	
	public static String model2datastring_inv(AbstractModel<?> model)
	{
		String modelstring = model.toString();
		modelstring = modelstring.replace("[", "");
		modelstring = modelstring.replace("]", "");
		modelstring = modelstring.replace("AffineTransform", "");
		String[] modelvalues = modelstring.split("[,()]");
		String datastring = "";
		
		//convert to mpicbg.trakem2.transform.AffineModel2D
		String classname = "mpicbg.trakem2.transform.AffineModel2D";
		
		Float tr_x = Float.parseFloat(modelvalues[4]) * -1;
		Float tr_y = Float.parseFloat(modelvalues[7]) * -1;
		datastring = modelvalues[2] + " " + modelvalues[3] + " " + modelvalues[5] + " " + modelvalues[6] + " " + Float.toString(tr_x) + " " + Float.toString(tr_y);
		return datastring;
	}
	
	
	public static void settilespecs(List<TileSpec> tileSpecs,ResolvedTileSpecCollection resolvedtileSpecs )
	{
		for (int i = 0; i < tileSpecs.size(); i++)
  		{
			TileSpec T = resolvedtileSpecs.getTileSpec(tileSpecs.get(i).getTileId()); 
			T.flattenTransforms();
			LeafTransformSpec r = (LeafTransformSpec)T.getLastTransform();
			ListTransformSpec listtransforms = new ListTransformSpec();
			listtransforms.addSpec(r);
			tileSpecs.get(i).setTransforms(listtransforms);
  		}
	}
	
	public static final int POOL_TIMEOUT_IN_SECONDS = 2000;
    public static final int STD_THREAD_POOL_SIZE = 16;
		
	
	public static void main( final String[] args ) throws Exception
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
        	
        	//System.out.println("DATASTRING: ");
		//System.exit(0);
  		//List<TileSpec> inputtileSpecs = readtilespecs(params.inputtilespec);
		List<TileSpec> inputtileSpecs = loadTileData(params.inputtilespec);
		List<TransformSpec> inputTransforms = loadTransformData(params.inputtransformspec);
		ResolvedTileSpecCollection resolvedTilesinput = new ResolvedTileSpecCollection(inputTransforms, inputtileSpecs);

		List<TileSpec> referencetileSpecs = loadTileData(params.referencetilespec);
		List<TransformSpec> referenceTransforms = loadTransformData(params.referencetransformspec);
		ResolvedTileSpecCollection resolvedTilesref = new ResolvedTileSpecCollection(referenceTransforms, referencetileSpecs);

		
		List<TileSpec> referencetileSpecs1 = loadTileData(params.referencetilespec);
		final List<TileSpec> outputtileSpecs = loadTileData(params.inputtilespec);
		
		
		AbstractModel <?> model = new RigidModel2D();
		//model = calculatetransform(resolvedTilesref, resolvedTilesinput, referencetileSpecs, inputtileSpecs, params);
		model = calculatetransform(resolvedTilesinput, resolvedTilesref, inputtileSpecs, referencetileSpecs,  params);
		//model = calculatetransform(resolvedTilesinput, resolvedTilesinput, inputtileSpecs, inputtileSpecs, params);
		
		
		String classname = "mpicbg.trakem2.transform.AffineModel2D";
		System.out.println("After returning....");
		System.out.println(model.toString());
		//String modstring = model.toString();
  		String modstring = model2datastring_inv(model);
  		System.out.println("After inverting....");
  		System.out.println(modstring);
  		
  		LeafTransformSpec modtrans = new LeafTransformSpec(classname,modstring);
  		
		//tile pair matching
      	ArrayList<TileBounds> tbl = new ArrayList();
      	Map<String,Integer> tsHashMap = new HashMap<String,Integer> ();
      	for (int i = 0; i < referencetileSpecs.size(); i ++)
      	{
      		TileSpec t =referencetileSpecs.get(i); 
      		t.deriveBoundingBox(t.getMeshCellSize(), true);
      		TileBounds tb = new TileBounds(t.getTileId(), t.getMinX(), t.getMinY(), t.getMaxX(), t.getMaxY());
      		tbl.add(tb);
      		tsHashMap.put(t.getTileId(),i);
      	}
      	
      	
      	settilespecs(inputtileSpecs, resolvedTilesinput);
      	settilespecs(referencetileSpecs, resolvedTilesref);
      	for (int j = 0; j < inputtileSpecs.size(); j++)
      	{
      		ListTransformSpec listtspec = inputtileSpecs.get(j).getTransforms();
      		listtspec.addSpec(modtrans);
      	}
      	
      	ArrayList<Integer> badindices = new ArrayList<Integer>();
      	ExecutorService tilePool = Executors.newFixedThreadPool(STD_THREAD_POOL_SIZE);
      	for (int tempj = 0; tempj < inputtileSpecs.size(); tempj ++)
      	{
				final int j = tempj;
				TileBoundsRTree tree = new TileBoundsRTree(referencetileSpecs.get(0).getZ(),tbl);
				TileSpec u =inputtileSpecs.get(j);
				u.deriveBoundingBox(u.getMeshCellSize(), true);
				List<TileBounds> tileBoundsList  = tree.findTilesInBox(u.getMinX(), u.getMinY(), u.getMaxX(), u.getMaxY());
					
				//find closest tile
				if (tileBoundsList.size() < 1)
				{
						System.out.println("Tile bounds not found...");
						badindices.add(tempj);
				}
				
				else
				{
			
					ArrayList <Double> distsq = new ArrayList(); 
					for (int i = 0; i < tileBoundsList.size(); i++)
					{
						double sum = Math.pow(u.getMinX() - tileBoundsList.get(i).getMinX(),2) + Math.pow(u.getMinY() - tileBoundsList.get(i).getMinY(),2) + Math.pow(u.getMaxX() - tileBoundsList.get(i).getMaxX(),2) + Math.pow(u.getMaxY() - tileBoundsList.get(i).getMaxY(),2);  
						distsq.add(sum );	
					}
							
					int index = distsq.indexOf(Collections.min(distsq));
					TileSpec reftile = referencetileSpecs1.get(tsHashMap.get(tileBoundsList.get(index).getTileId()));
					ArrayList<TileSpec> ref = new ArrayList(); ref.add(reftile);
					ArrayList<TileSpec> inp = new ArrayList(); inp.add(inputtileSpecs.get(j));
					
							
					params.percentSaturated = 0.9f;
					params.maxEpsilon = 2.5f;
					params.initialSigma = 1.0f;
					//params.maxOctaveSize = 200;
					//params.minOctaveSize = 0;
					
					
					final Params jparams = params;
					final AbstractModel<?> jmodel = model;
					final String jclassname = classname;
					final TileSpec jreftile = reftile;
					final ArrayList<TileSpec> jref = ref;
					final ArrayList<TileSpec> jinp = inp;
					
					Runnable submissible = new Runnable() {
						public void run() {
							
							AbstractModel<?> tilemodel = new RigidModel2D();
							tilemodel = calculatetransform(jref,jinp, jparams,(RigidModel2D)jmodel);		
							
							String datastring = model2datastring(tilemodel);
							System.out.println("DATASTRING: "+ datastring);	
							System.out.println("MYTILELIST1: ");
							System.out.println("MYTILELIST11: " + jclassname);						
							LeafTransformSpec tilelts = new LeafTransformSpec(jclassname, datastring);
							System.out.println("MYTILELIST111: " + tilelts.toJson());
							System.out.println("MYTILELIST1111: " + jreftile.getLastTransform().toJson());
							//ReferenceTransformSpec rfspec = (ReferenceTransformSpec) jreftile.getLastTransform();
							LeafTransformSpec rfspec = (LeafTransformSpec) jreftile.getLastTransform();
							System.out.println("MYTILELIST2: ");
							//ReferenceTransformSpec refspec = new ReferenceTransformSpec(rfspec.getEffectiveRefId());
							ListTransformSpec mytilelist = new ListTransformSpec(jparams.referenceID,null);
							System.out.println("MYTILELIST3: ");							
							mytilelist.addSpec(tilelts);
							//mytilelist.addSpec(refspec); // when using an input of a reference spec
							mytilelist.addSpec(rfspec);						
							System.out.println("MYTILELIST: ");
							System.out.println(mytilelist.toString());
							outputtileSpecs.get(j).setTransforms(mytilelist);
							
						}
					};
					tilePool.submit(submissible);
				}
				
		

			}
			
		
			tilePool.shutdown();
			try {
				tilePool.awaitTermination(POOL_TIMEOUT_IN_SECONDS,TimeUnit.SECONDS);
			} 
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      	
      	/*ArrayList<Integer> badindices = new ArrayList<Integer>();
      
      	for (int tempj = 0; tempj < inputtileSpecs.size(); tempj ++)
      	{
			try{
				final int j = tempj;
				System.out.println("This is j: " + j);
				TileBoundsRTree tree = new TileBoundsRTree(referencetileSpecs.get(0).getZ(),tbl);
				TileSpec u =inputtileSpecs.get(j);
				u.deriveBoundingBox(u.getMeshCellSize(), true);
				List<TileBounds> tileBoundsList  = tree.findTilesInBox(u.getMinX(), u.getMinY(), u.getMaxX(), u.getMaxY());
					
				//find closest tile
				ArrayList <Double> distsq = new ArrayList(); 
				System.out.println("This is tileboundslist size: " + tileBoundsList.size());
				
				final String jclassname = classname;
				
				final Params jparams = params;
				String datastring = null;
				if (tileBoundsList.size() > 0)
				{
					for (int i = 0; i < tileBoundsList.size(); i++)
					{
						System.out.println("This is i: " + i);
						double sum = Math.pow(u.getMinX() - tileBoundsList.get(i).getMinX(),2) + Math.pow(u.getMinY() - tileBoundsList.get(i).getMinY(),2) + Math.pow(u.getMaxX() - tileBoundsList.get(i).getMaxX(),2) + Math.pow(u.getMaxY() - tileBoundsList.get(i).getMaxY(),2);  
						distsq.add(sum );	
						System.out.println("This is sum: " + sum);
					}
							
					int index = distsq.indexOf(Collections.min(distsq));
					TileSpec reftile = referencetileSpecs1.get(tsHashMap.get(tileBoundsList.get(index).getTileId()));
				
					ArrayList<TileSpec> ref = new ArrayList(); ref.add(reftile);
					ArrayList<TileSpec> inp = new ArrayList(); inp.add(inputtileSpecs.get(j));
					
							
					params.percentSaturated = 0.9f;
					params.maxEpsilon = 2.5f;
					params.initialSigma = 1.0f;
					//params.maxOctaveSize = 200;
					//params.minOctaveSize = 0;
					
					
					
					final AbstractModel<?> jmodel = model;
					
					final ArrayList<TileSpec> jref = ref;
					final ArrayList<TileSpec> jinp = inp;
					final TileSpec jreftile = reftile;
					
					AbstractModel<?> tilemodel = new RigidModel2D();
					tilemodel = calculatetransform(jref,jinp, jparams,(RigidModel2D)jmodel);		
							
					datastring = model2datastring(tilemodel);
					System.out.println("Printing datastring: ");
					System.out.println(datastring);
					LeafTransformSpec tilelts = new LeafTransformSpec(jclassname, datastring);
					ReferenceTransformSpec rfspec = (ReferenceTransformSpec) jreftile.getLastTransform();
					ReferenceTransformSpec refspec = new ReferenceTransformSpec(rfspec.getEffectiveRefId());
					ListTransformSpec mytilelist = new ListTransformSpec(jparams.referenceID,null);
					mytilelist.addSpec(tilelts);
					mytilelist.addSpec(refspec);
					outputtileSpecs.get(j).setTransforms(mytilelist);
						
					System.out.println("This is output tilespec: ");
					System.out.println(outputtileSpecs.get(j).toString());
					System.out.println(mytilelist.toString());
					System.out.println("--------------------------------");
				}
				else 
				{
						badindices.add(tempj);
				}
				
				
				
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}*/
		
		
		
		
		
		
		
		
      	
      	//remove tilespecs with bad indices (tiles with no overlapping tiles in the ref image)
		/*Collections.sort(badindices);
		Collections.reverse(badindices);
		List modifiableoutlist = new List(Arrays.asList)
      	for (int bi : badindices)
      	{
			System.out.println("This is bi: ");
			System.out.println(bi);
			outputtileSpecs.remove(bi);
			
		}*/
      	
      	final StringBuilder json = new StringBuilder(16 * 1024);
      	System.out.println("NOW OUTPUTTING TO FILE: ");
      	int numadded = 0;
      	for (int n = 0; n< outputtileSpecs.size(); n++)
      	{

			System.out.println(n);
			if (badindices.contains(n))
				System.out.println("Skipping "+n);
			else
			{
				String myjson =outputtileSpecs.get(n).toJson() ;
				

				//System.out.println(myjson);

				if (numadded >0 )
					myjson = "," + myjson;
				json.append(myjson);
				numadded = numadded+1;
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
  		
      	//testcomment
  		
		
	}
  		
  		
}
