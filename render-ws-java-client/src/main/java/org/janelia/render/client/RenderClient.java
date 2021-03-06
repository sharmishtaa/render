package org.janelia.render.client;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.janelia.alignment.RenderParameters;
import org.janelia.render.client.response.BufferedImageResponseHandler;
import org.janelia.render.client.response.FileResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Java client for accessing Render Web Services.
 *
 * @author Eric Trautman
 */
public class RenderClient {

    public static final String DEFAULT_FORMAT = RenderClientParameters.JPEG_FORMAT.toLowerCase();

    /**
     * @param  args  see {@link RenderClientParameters} for command line argument details.
     */
    public static void main(String[] args) {
        try {
            final RenderClientParameters clientParameters = RenderClientParameters.parseCommandLineArgs(args);

            if (clientParameters.displayHelp()) {
                clientParameters.showUsage();
            } else {
                final RenderClient client = new RenderClient(clientParameters.getBaseUri(),
                                                             clientParameters.getProjectId());
                if (clientParameters.renderInWindow()) {
                    client.renderInWindow(clientParameters.getIn(),
                                          clientParameters.getFormat());
                } else {
                    client.renderToFile(clientParameters.getIn(),
                                        clientParameters.getOut(),
                                        clientParameters.getFormat());
                }
            }
        } catch (Throwable t) {
            LOG.error("main: caught exception", t);
            System.exit(1);
        }
    }

    private Map<String, URI> formatToRenderUriMap;
    private CloseableHttpClient httpClient;

    /**
     * @param  baseUriString  base URI for web service (e.g. http://renderer.int.janelia.org/render-ws/v1).
     * @param  owner          owner (e.g. 'flyTEM') for all requests.
     *
     * @throws IllegalArgumentException
     *   if the render URI instances cannot be constructed.
     */
    public RenderClient(String baseUriString,
                        String owner) throws IllegalArgumentException {

        final String trimmedBaseUriString;
        if (baseUriString.endsWith("/")) {
            trimmedBaseUriString = baseUriString.substring(0, baseUriString.length() - 1);
        } else {
            trimmedBaseUriString = baseUriString;
        }

        final String projectUriString = trimmedBaseUriString + "/owner/" + owner;
        final URI jpegUri = createUri(projectUriString + "/jpeg-image");
        final URI pngUri = createUri(projectUriString + "/png-image");
        final Map<String, URI> map = new LinkedHashMap<>();
        map.put(RenderClientParameters.JPEG_FORMAT.toLowerCase(), jpegUri);
        map.put(RenderClientParameters.PNG_FORMAT.toLowerCase(), pngUri);
        this.formatToRenderUriMap = map;

        this.httpClient = HttpClients.createDefault();
    }

    /**
     * @param  format  image format.
     *
     * @return render URI for the specified format.
     */
    public URI getRenderUriForFormat(String format) {

        String lowerCaseFormat;
        if (format == null) {
            lowerCaseFormat = DEFAULT_FORMAT;
        } else {
            lowerCaseFormat = format.toLowerCase();
        }

        URI uri = formatToRenderUriMap.get(lowerCaseFormat);
        if (uri == null) {
            LOG.warn("getRenderUriForFormat: unknown format '" + format + "' requested, using '" +
                     DEFAULT_FORMAT + "' instead, known formats are: " + formatToRenderUriMap.keySet());
            uri = formatToRenderUriMap.get(DEFAULT_FORMAT);
        }

        return uri;
    }

    @Override
    public String toString() {
        return "RenderClient{formatToRenderUriMap=" + formatToRenderUriMap + '}';
    }

    /**
     * Invokes the Render Web Service using parameters loaded from the specified file and
     * writes the resulting image to the specified output file.
     *
     * @param  renderParametersPath  path of render parameters (json) file.
     * @param  outputFilePath        path of result image file.
     * @param  outputFormat          format of result image (e.g. 'jpeg' or 'png').
     *
     * @throws IOException
     *   if the render request fails for any reason.
     */
    public void renderToFile(String renderParametersPath,
                             String outputFilePath,
                             String outputFormat)
            throws IOException {

        final File parametersFile = new File(renderParametersPath);
        final RenderParameters renderParameters = RenderParameters.parseJson(parametersFile);

        final File outputFile = new File(outputFilePath).getCanonicalFile();

        renderToFile(renderParameters, outputFile, outputFormat);
    }

    /**
     * Invokes the Render Web Service using specified parameters and
     * writes the resulting image to the specified output file.
     *
     * @param  renderParameters  render parameters.
     * @param  outputFile        result image file.
     * @param  outputFormat      format of result image (e.g. 'jpeg' or 'png').
     *
     * @throws IOException
     *   if the render request fails for any reason.
     */
    public void renderToFile(RenderParameters renderParameters,
                             File outputFile,
                             String outputFormat)
            throws IOException {

        LOG.info("renderToFile: entry, renderParameters={}, outputFile={}, outputFormat={}",
                 renderParameters, outputFile, outputFormat);

        if (outputFile.exists()) {
            if (! outputFile.canWrite()) {
                throw new IOException("output file " + outputFile.getAbsolutePath() + " cannot be overwritten");
            }
        } else {
            File parentFile = outputFile.getParentFile();
            while (parentFile != null) {
                if (parentFile.exists()) {
                    if (! parentFile.canWrite()) {
                        throw new IOException("output file cannot be written to parent directory " +
                                              parentFile.getAbsolutePath());
                    }
                    break;
                }
                parentFile = parentFile.getParentFile();
            }
        }

        final String json = renderParameters.toJson();
        final StringEntity stringEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
        final URI uri = getRenderUriForFormat(outputFormat);
        final FileResponseHandler responseHandler = new FileResponseHandler("PUT " + uri,
                                                                            outputFile);
        final HttpPut httpPut = new HttpPut(uri);
        httpPut.setEntity(stringEntity);

        httpClient.execute(httpPut, responseHandler);

        LOG.info("renderToFile: exit, wrote image to {}", outputFile.getAbsolutePath());
    }

    /**
     * Invokes the Render Web Service using parameters loaded from the specified file and
     * displays the resulting image in a window.
     *
     * @param  renderParametersPath  path of render parameters (json) file.
     * @param  outputFormat          format of result image (e.g. 'jpeg' or 'png').
     *
     * @throws IOException
     *   if the render request fails for any reason.
     */
    public void renderInWindow(String renderParametersPath,
                               String outputFormat)
            throws IOException {
        final File parametersFile = new File(renderParametersPath);
        final RenderParameters renderParameters = RenderParameters.parseJson(parametersFile);
        renderInWindow(renderParameters, outputFormat);
    }

    /**
     * Invokes the Render Web Service using specified parameters and
     * displays the resulting image in a window.
     *
     * @param  renderParameters  render parameters.
     * @param  outputFormat      format of result image (e.g. 'jpeg' or 'png').
     *
     * @throws IOException
     *   if the render request fails for any reason.
     */
    public void renderInWindow(RenderParameters renderParameters,
                               String outputFormat)
            throws IOException {

        LOG.info("renderInWindow: entry, renderParameters={}, outputFormat={}", renderParameters, outputFormat);

        final String json = renderParameters.toJson();
        final StringEntity stringEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
        final URI uri = getRenderUriForFormat(outputFormat);
        final BufferedImageResponseHandler responseHandler = new BufferedImageResponseHandler("PUT " + uri);
        final HttpPut httpPut = new HttpPut(uri);
        httpPut.setEntity(stringEntity);

        final BufferedImage image = httpClient.execute(httpPut, responseHandler);

        final ScrollableImageWindow window = new ScrollableImageWindow(renderParameters.toString(), image);
        window.setVisible(true);

        LOG.info("renderInWindow: exit, created {}", image);
    }

    private URI createUri(String uriString) throws IllegalArgumentException {
        URI uri;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("failed to parse URI string: " + uriString, e);
        }
        return uri;
    }

    private static final Logger LOG = LoggerFactory.getLogger(RenderClient.class);
}
