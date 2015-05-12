package gate.creole.segmenter;


import edu.stanford.nlp.util.CoreMap;
import gate.AnnotationSet;
import gate.Factory;
import gate.FeatureMap;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.InvalidOffsetException;
import gate.util.SimpleFeatureMapImpl;
import org.apache.xpath.SourceTree;

import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import static edu.stanford.nlp.ling.CoreAnnotations.*;

@CreoleResource(name = "Stanford Chinese Segmenter", comment = "Stanford Chinese Segmenter.", icon = "shefTokeniser")
public class ChineseSegmenter extends AbstractLanguageAnalyser {


    public static final String TAG_DOCUMENT_PARAMETER_NAME = "document";

    public static final String TAG_INPUT_AS_PARAMETER_NAME = "inputASName";

    public static final String TAG_ENCODING_PARAMETER_NAME = "encoding";

    public static final String TAG_OUTPUT_AS_PARAMETER_NAME = "outputASName";

    public static final String TOKEN_LABEL = "tokenLabel";

    public static final String SPACE_LABEL = "spaceLabel";

    public static final String Split_LABEL = "splitLabel";

    public static final String TOKEN_STRING_FEATURE = "string";

    public ChineseSegmenter() {
    }

    @Override
    public Resource init() throws ResourceInstantiationException {
        return this;
    }

    @Override
    public void reInit() throws ResourceInstantiationException {
        init();
    }

    @Override
    public void execute() throws ExecutionException {
        try {
            if (document == null)
                throw new ExecutionException("No document to process!");

            AnnotationSet inputAS = document.getAnnotations(inputASName);
            AnnotationSet outputAS = document.getAnnotations(outputASName);

            long startTime = System.currentTimeMillis();
            fireStatusChanged("Segmenting " + document.getName());
            fireProgressChanged(0);

            Properties props = new Properties();
            props.setProperty("annotators", "segment");
            props.setProperty("customAnnotatorClass.segment", "edu.stanford.nlp.pipeline.ChineseSegmenterAnnotator");
            props.setProperty("segment.model", modelFile.toString().substring(5));
            props.setProperty("segment.sighanCorporaDict", dictDir.toString().substring(5));
            props.setProperty("segment.serDictionary", dictFile.toString().substring(5));
            props.setProperty("segment.sighanPostProcessing", "true");
            props.setProperty("segment.verbose", "false");
//            props.setProperty("ssplit.boundaryTokenRegex", "[.]|[!?]+|[。]|[！？]+");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            String content = document.getContent().getContent(new Long(0), document.getContent().size()).toString();

//            String[] paragrams = getParagrams(content);
//            int paragramcount = paragrams.length;
//            int paragramlengh = 0;
//            for (int index = 0; index < paragramcount; index++) {
//                String paragram = paragrams[index];
//
//                SetSplitFeatures(paragramlengh, paragram);
//
//                paragramlengh += paragram.length() + paragramflagLength;
//            }

            Annotation doc = new Annotation(content);
            pipeline.annotate(doc);

            Long tokenStart;
            Long tokenEnd;
            Long prevTokenEnd = new Long(0);

            List<CoreLabel> tokens = doc.get(TokensAnnotation.class);

            for (CoreLabel token : tokens) {
                String word = token.get(TextAnnotation.class);
                tokenStart = new Long(token.get(CharacterOffsetBeginAnnotation.class));
                tokenEnd = new Long(token.get(CharacterOffsetEndAnnotation.class));

                SimpleFeatureMapImpl tokenMap = new SimpleFeatureMapImpl();

                // add the token annotation
                try {
                    tokenMap.put(TOKEN_STRING_FEATURE, word);
                    outputAS.add(tokenStart, tokenEnd, tokenLabel, tokenMap);
                } catch (InvalidOffsetException e) {
                    System.out.println("Token alignment problem:" + e);
                }

                // do we need to add a space annotation?
                if (tokenStart > prevTokenEnd) {
                    try {
                        outputAS.add(prevTokenEnd, tokenStart, spaceLabel, new SimpleFeatureMapImpl());
                    } catch (InvalidOffsetException e) {
                        System.out.println("Space token alignment problem:" + e);
                    }
                }

                prevTokenEnd = tokenEnd;
            }

            fireProcessFinished();
            fireStatusChanged(document.getName() +
                    " segmented in " +
                    NumberFormat.getInstance().format(
                            (double) (System.currentTimeMillis() - startTime) / 1000) +
                    " seconds!");
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    private String[] getParagrams(String content) {
        String sentenceSplit = "";
        // if the paragram flag is \r\n
        if (content.contains("\r\n")) {
            sentenceSplit = "\r\n";
            paragramflagLength = 2;
        } else {
            sentenceSplit = "\n";
            paragramflagLength = 1;
        }
        Pattern externalSplitsPattern = Pattern.compile(sentenceSplit);
        String[] paragrams = externalSplitsPattern.split(content);
        return paragrams;

    }

//    protected int getNoSpaceIdx(int startIdx) {
//        String content = document.getContent().toString();
//        if (startIdx >= content.length() - 1) {
//            return 0;
//        }
//        String spacestring = content.substring(startIdx, startIdx + 1);
//        while (spacestring != null && spacestring.equals(" ")) {
//            startIdx++;
//            spacestring = content.substring(startIdx, startIdx + 1);
//        }
//        return startIdx;
//    }

//    private boolean SetSplitFeatures(int paragramstart, String paragram) throws InvalidOffsetException {
//        // get document's annotationset
//        AnnotationSet annotationSet;
//        if (outputASName == null ||
//                outputASName.equals("")) annotationSet = document.getAnnotations();
//        else annotationSet = document.getAnnotations(outputASName);
//
//        try {
//            FeatureMap features = Factory.newFeatureMap();
//            features.put("kind", "external");
//            annotationSet.add(new Long(paragramstart + paragram.length()), new Long(paragramstart + paragram.length() + paragramflagLength),
//                    "Split", features);
//        } catch (InvalidOffsetException ioe) {
//            return false;
//        }
//
//        return true;
//    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Optional
    @RunTime
    @CreoleParameter(comment = "Input annotation set name", defaultValue = "")
    public void setInputASName(String newInputASName) {
        inputASName = newInputASName;
    }

    public String getInputASName() {
        return inputASName;
    }

    public String getEncoding() {
        return this.encoding;
    }

    public String getOutputASName() {
        return this.outputASName;
    }

    @Optional
    @RunTime
    @CreoleParameter(comment = "Output annotation set name", defaultValue = "")
    public void setOutputASName(String outputASName) {
        this.outputASName = outputASName;
    }

    public String getTokenLabel() {
        return this.tokenLabel;
    }

    @Optional
    @RunTime
    @CreoleParameter(comment = "Annotation type for tokens", defaultValue = "Token")
    public void setTokenLabel(String tokenLabel) {
        this.tokenLabel = tokenLabel;
    }

    public String getSpaceLabel() {
        return this.spaceLabel;
    }

    @Optional
    @RunTime
    @CreoleParameter(comment = "Annotation type for spaces", defaultValue = "SpaceToken")
    public void setSpaceLabel(String spaceLabel) {
        this.spaceLabel = spaceLabel;
    }

    @CreoleParameter(comment = "Path to the segmenter model file", defaultValue = "resources/ctb.gz", suffixes = "tagger;model;gz")
    public void setModelFile(URL modelFile) {
        this.modelFile = modelFile;
    }

    public URL getModelFile() {
        return this.modelFile;
    }

    @CreoleParameter(comment = "Path to the segmenter dictionary file", defaultValue = "resources/dict-chris6.ser.gz", suffixes = "tagger;model;gz")
    public void setDictFile(URL dictFile) {
        this.dictFile = dictFile;
    }

    public URL getDictFile() {
        return this.dictFile;
    }

    @CreoleParameter(comment = "Path to the dictionary directory", defaultValue = "resources/")
    public void setDictDir(URL dictDir) {
        this.dictDir = dictDir;
    }

    public URL getDictDir() {
        return this.dictDir;
    }

//    @Optional
//    @RunTime
//    @CreoleParameter(comment = "Annotation type for spaces", defaultValue = "Split")
//    public void setSplitLabel(String splitLabel) {
//        this.splitLabel = splitLabel;
//    }
//
//    public String getSplitLabel() {
//        return this.splitLabel;
//    }

    private String inputASName;

    private String encoding;

    private String outputASName;

    private String tokenLabel;

    private String spaceLabel;

//    private String splitLabel;

    private URL modelFile;

    private URL dictFile;

    private URL dictDir;

    private int paragramflagLength = 1;

}