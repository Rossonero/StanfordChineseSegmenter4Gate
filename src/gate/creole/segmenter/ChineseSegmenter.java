package gate.creole.segmenter;


import edu.stanford.nlp.util.CoreMap;
import gate.AnnotationSet;
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

import static edu.stanford.nlp.ling.CoreAnnotations.*;

@CreoleResource(name = "Stanford Chinese Segmenter", comment = "Stanford Chinese Segmenter.", icon = "segmenter")
public class ChineseSegmenter extends AbstractLanguageAnalyser {


    public static final String TAG_DOCUMENT_PARAMETER_NAME = "document";

    public static final String TAG_INPUT_AS_PARAMETER_NAME = "inputASName";

    public static final String TAG_ENCODING_PARAMETER_NAME = "encoding";

    public static final String TAG_OUTPUT_AS_PARAMETER_NAME = "outputASName";

    public static final String TOKEN_LABEL = "tokenLabel";

    public static final String SPACE_LABEL = "spaceLabel";

    public static final String TOKEN_STRING_FEATURE = "string";

    public ChineseSegmenter() {
    }

    @Override
    public Resource init() throws ResourceInstantiationException {
        System.out.println("33");
        return this;
    }

    @Override
    public void reInit() throws ResourceInstantiationException {
        init();
    }

    @Override
    public void execute() throws ExecutionException {
        try {
            System.out.println("44");
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
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            String content = document.getContent().getContent(new Long(0), document.getContent().size()).toString();
            System.out.println("-----------");
            System.out.println(content);
            System.out.println("-----------");
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
                    System.out.println(word + " s: " + tokenStart + " e: " + tokenEnd + " " + tokenMap);
                    outputAS.add(tokenStart, tokenEnd, tokenLabel, tokenMap);
                } catch (InvalidOffsetException e) {
                    System.out.println("Token alignment problem:" + e);
                }

                // do we need to add a space annotation?
                if (tokenStart > prevTokenEnd) {
                    try {
                        outputAS.add(prevTokenEnd, tokenStart, "SpaceToken", new SimpleFeatureMapImpl());
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

    private String inputASName;

    private String encoding;

    private String outputASName;

    private String tokenLabel;

    private String spaceLabel;

    private URL modelFile;

    private URL dictFile;

    private URL dictDir;

}

//public class Main {
//
//    public static void main(String[] args) {
//
//        Properties props = new Properties();
//        props.setProperty("annotators", "segment, ssplit, pos, ner");
//        props.setProperty("customAnnotatorClass.segment", "edu.stanford.nlp.pipeline.ChineseSegmenterAnnotator");
//        props.setProperty("segment.model", "edu/stanford/nlp/models/segmenter/chinese/ctb.gz");
//        props.setProperty("segment.sighanCorporaDict", "edu/stanford/nlp/models/segmenter/chinese");
//        props.setProperty("segment.serDictionary", "edu/stanford/nlp/models/segmenter/chinese/dict-chris6.ser.gz");
//        props.setProperty("segment.sighanPostProcessing", "true");
//        props.setProperty("ssplit.boundaryTokenRegex", "[.]|[!?]+|[。]|[！？]+");
//        props.setProperty("pos.model", "edu/stanford/nlp/models/pos-tagger/chinese-distsim/chinese-distsim.tagger");
//        props.setProperty("ner.model", "edu/stanford/nlp/models/ner/chinese.misc.distsim.crf.ser.gz");
//        props.setProperty("ner.applyNumericClassifiers", "false");
//        props.setProperty("ner.useSUTime", "false");
//        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
////        read some text in the text variable
////        String text = "01 3 5  89 123 56    123 56";
//        String text = "应聘职位： 技术总监（上海-卢湾区）\n" +
//                "应聘公司： 上海锦江国际电子商务有限公司\n" +
//                "投递时间： 2014-08-12\n" +
//                "简历匹配度：\n" +
//                "100%\n" +
//                "\n" +
//                "\n" +
//                "简        历\n" +
//                "简历关键字： 总监/经理\n" +
//                "穆克良\n" +
//                "8-9年工作经验  |  男 |  32岁(1982年7月28日) (ID:4609345)\n" +
//                "居住地： 上海-长宁区 户 口： 运城\n" +
//                "地 址： 上海长宁区新华路 （邮编：100086）\n" +
//                "电 话： 15026758640（手机）\n" +
//                "E-mail： mukeliang@163.com\n" +
//                "\n" +
//                "最近工作 [ 4年3个月]\n" +
//                "公 司： 上海赵涌电子商务服务有限公司\n" +
//                "行 业： 互联网/电子商务\n" +
//                "职 位： 技术总监/经理\n" +
//                "最高学历\n" +
//                "学 历： 本科\n" +
//                "专 业： 电子商务\n" +
//                "学 校： 北京联合大学\n" +
//                "\n" +
//                "目前年薪： 15-30万人民币\n" +
//                "\n" +
//                "自我评价\n" +
//                "个性开朗,有创意能力和较好的沟通能力，有较强的组织和协调能力，有极高的工作热情和强烈的责任心\n" +
//                "精力充沛,沉着冷静,全局战略意识,团队协作能力以及创新精神\n" +
//                "十年以上JAVA工作经验，五年以上项目管理经验，擅长团队组建与管理，有丰富的跨部门合作经验\n" +
//                "熟悉企业应用软件的开发及电子商务网站的开发，熟悉互联网行业。\n" +
//                "精通JAVA，熟悉php、oc，深入理解OOP思想,熟悉设计模式\n" +
//                "熟练运用J2EE技术及常用框架技术(Struts,Spring,Hibernate,iBatis等)\n" +
//                "熟练运用MySQL，SQL Server，Oracle等关系型数据库及NoSQL，对Oracle性能优化有深入研究\n" +
//                "熟练运用Lucene,Solr搜索引擎技术\n" +
//                "熟悉iphone开发，对android有一定了解\n" +
//                "\n" +
//                "求职意向\n" +
//                "到岗时间： 一个月内\n" +
//                "工作性质： 全职\n" +
//                "希望行业： 计算机软件\n" +
//                "目标地点： 上海\n" +
//                "期望薪水： 30000-49999/月\n" +
//                "目标职能： 技术总监/经理，首席技术执行官CTO/首席信息官CIO\n" +
//                "\n" +
//                "工作经验\n" +
//                "2010 /5--至今：上海赵涌电子商务服务有限公司（150-500人） [ 4年3个月]\n" +
//                "所属行业： 互联网/电子商务\n" +
//                "技术部          技术总监/经理\n" +
//                "担任技术总监，组建技术团队、项目管理、架构搭建、系统分析及设计、核心模块开发\n" +
//                "汇报对象： 总经理\n" +
//                "下属人数： 21人\n" +
//                "2009 /5--2010 /5：太极计算机股份有限公司（1000-5000人） [ 1年]\n" +
//                "所属行业： 计算机软件\n" +
//                "政府事业部          项目经理\n" +
//                "担任项目经理，负责广电总局无线局运行管理系统项目的需求分析,系统设计，数据库设计及核心用例的开发及团队管理\n" +
//                "汇报对象： 部门经理\n" +
//                "下属人数： 8人\n" +
//                "2004 /12--2009 /4：中软股份有限公司 [ 4年4个月]\n" +
//                "所属行业： 计算机软件\n" +
//                "税务事业部          高级软件工程师\n" +
//                "负责公司核心业务模块的分析设计及编码，对项目组的同事进行技术支持和培训。自加入公司以来，参与了公司诸多关键性项目，在这些项目中担当重要角色，出色的完成了工作，得到公司领导的认可\n" +
//                "\n" +
//                "项目经验\n" +
//                "2010 /12--至今： 赵涌在线移动项目\n" +
//                "项目描述： 目前人们对手机的依赖程度越来越高，手机上网速度越来越快，公司决定向移动互联网进军（包括Wap版、iphone版、android版）。Wap版的网站，项目中用到的技术，Lucene搜索引擎（因是拍卖网站，10秒钟索引及时更新），框架采用Spring+Struts2+ibatis,数据库是oracle，考虑并发量较大，SQL语句都经过性能测试;iphone,android版本目前已经上线。\n" +
//                "业务模块包括竞拍管理、订单管理、支付管理、物流管理等模块\n" +
//                "责任描述： 负责团队建设、和其它部门沟通、架构设计、需求分析、系统设计、核心用例的开发。\n" +
//                "2010 /12--至今： 赵涌在线后台管理系统\n" +
//                "项目描述： 公司ERP开发，框架采用Spring+Struts2+hibernate，前台技术是EXT。业务模块包括权限管理、工作流管理、通用查询、合同管理、财务管理等模块\n" +
//                "责任描述： 负责团队建设、和其它部门沟通、架构设计、需求分析、系统设计、核心用例的开发。\n" +
//                "2010 /5--2010 /11： 赵涌在线网站\n" +
//                "项目描述： 负责公司网站、后台系统的维护及性能优化；网上商城项目的管理、需求分析及设计、核心用例的开发\n" +
//                "责任描述： 负责团队建设、和其它部门沟通、架构设计、需求分析、系统设计、核心用例的开发。\n" +
//                "2009 /5--2010 /5： 无线局运行管理系统\n" +
//                "硬件环境： windows\n" +
//                "开发工具： eclipse\n" +
//                "项目描述： 运行管理系统是无线局的生产管理系统，因系统的实时性要求高，图形化监监控界面比较多，项目采用C/S架构，用多线程技术来处理核心业务，用eclipse rcp+hibernate+oracle技术开发\n" +
//                "责任描述： 负责团队管理、调度令模块及代播模块的需求分析及设计，并完成核心用例的开发。工作期间，可以很好的和客户沟通，并提取出需求书写需求规格说明书，根据需求进行书写概要设计、详细设计、界面设计、数据库设计的文档\n" +
//                "2007 /10--2009 /3： 重庆地税征管系统项目\n" +
//                "项目描述： 项目规模60人，根据各省的业务差异对产品进行二次开发和维护，主要用到的技术，前台页面用jsp,javascript,ajax开发，后台用springMVC,hibernate开发,数据库用oracle。其间，使本人在项目管理方面有了一定经验，交际能力又有了很大的提高，丰富了自己在WEB项目开发的经验，对oracle数据库应用优化和复杂存储过程的编写有了更进一步的提高\n" +
//                "责任描述： 本人和另外一个同事，两个人一块负责申报组用例的业务分析及申报组项目进度的安排，帮助组内其它同事解决技术难点、本组重要用例的编码，前台页面公共组件的开发，和重庆现场项目负责人、重庆地税客户的沟通，交流\n" +
//                "2007 /2--2007 /10： 吉林省税务征管系统项目\n" +
//                "项目描述： 适用整个吉林省税务局纳税 ,项目规模30-50人，本项目前台用GUI展示，底层架构是中软与IBM合作开发，主要用到技术JAVA AWT, SWING,oracle等技术。\n" +
//                "责任描述： 本人负责申报征收模块的开发,负责主要用例减免税用例，延期纳税用例等等\n" +
//                "2006 /5--2007 /2： 上海财税征管软件\n" +
//                "软件环境： linux\n" +
//                "开发工具： eclipse+Dreamweaver+powerDesiner+rose\n" +
//                "项目描述： 上海市财税局为提高上海市税务管理水平而开发基于web的软件，适用于全上海市各个税务局进行纳税。是公司从税务行业GUI产品转向WEB产品，项目规模100人左右。本项目采用中软公司自主研发的一套架构，系统中主要用到技术jsp,ajax,javascript,java,hibernate,spring,oracle等技术\n" +
//                "责任描述： 本人主要负责征管系统核心模块，申报征收模块的开发，开发典型用例有打印缴款书，现金汇总缴款书，印花税汇总缴款书，作废缴款书等等\n" +
//                "2004 /12--2006 /4： 山西地税项目开发\n" +
//                "项目描述： 山西省地税大集中项目\n" +
//                "责任描述： 主要负责申报征收用例的编写，用到SSH框架\n" +
//                "\n" +
//                "所获奖项\n" +
//                "2013 /9 奖学金 三等\n" +
//                "2013 /9 优秀学生共青团干部\n" +
//                "2013 /4 第六届“挑战杯”西北民族大学大学生课外学术科技作品竞赛 二等奖\n" +
//                "2012 /12 数学建模 校内三等奖\n" +
//                "2012 /9 奖学金 二等\n" +
//                "2012 /8 暑期“三下乡”社会实践活动 先进个人\n" +
//                "2012 /8 中国大学生计算机设计大赛 优胜奖\n" +
//                "2011 /9 奖学金 三等\n" +
//                "2011 /9 文体活动优胜奖\n" +
//                "2011 /4 西北民族大学首届校园绿色文化节身边化学知识竞赛 二等奖\n" +
//                "\n" +
//                "社会经验\n" +
//                "2012 /8--2012 /8 暑期“三下乡”社会实践活动 1.开展文化宣传，科技调研等活动\n" +
//                "2.普及计算机安全常识\n" +
//                "\n" +
//                "校内职务\n" +
//                "2011 /9--2012 /9 院学生会副主席 在步入大学之际，为了使大学生活更加丰富多彩，与2010年9月加入我们学院的团委学生会，从委员做起，我兢兢业业的参加学院组织的各种活动。在一年的锻炼中，被聘为组织部部长，后又成为院学生会副主席......\n" +
//                "\n" +
//                "教育经历\n" +
//                "2000 /9--2004 /7 北京联合大学 电子商务 本科\n" +
//                "在校期间曾学习电子商务网站开发，电子商务金融，电子商务概论，电子商务与现代物流，电子商务英语，经济学，商务交流，市场营销，会计学，数量方法等课程，学到了扎实得专业知识和应用能力。\n" +
//                "\n" +
//                "语言能力\n" +
//                "英语（熟练）：\n" +
//                "\n" +
//                "IT技能\n" +
//                "技能名称 熟练程度 使用时间\n" +
//                "Java 熟练 72月  \n" +
//                "Linux 一般 12月\n" +
//                "SQL Server 一般 24月\n" +
//                "Oracle 熟练 60月\n" +
//                "\n" +
//                "\n" +
//                "<resume.html>\n";
////        create an empty Annotation just with the given text
//        Annotation document = new Annotation(text);
//
//        // run all Annotators on this text
//        pipeline.annotate(document);
//        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
//        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
//        List<CoreLabel> tokens = document.get(TokensAnnotation.class);
//        for (CoreLabel token : tokens) {
////            this is the text of the token
//            String word = token.get(TextAnnotation.class);
//            long startStart = token.get(CharacterOffsetBeginAnnotation.class);
//            long startEnd = token.get(CharacterOffsetEndAnnotation.class);
//            String pos = token.get(PartOfSpeechAnnotation.class);
//            String ne = token.get(NamedEntityTagAnnotation.class);
//
//            System.out.println(word + "/" + pos + "/" + ne);
//        }
//    }
//}
