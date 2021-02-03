package net.luculent;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class MySpider {
    public static Vector<String> titles = new Vector<String>();//存放文章标题
    public static Vector<String> t_contents = new Vector<String>();//存放文章内容

    private static ConcurrentHashMap<String, ArrayList<String>> titleLinkContent;

    private static ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("demo-pool-%d").build();

    private static ExecutorService pool = new ThreadPoolExecutor(9, 20,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());


    public static class SpiderRun implements Runnable {
        private String url;
        private String key;
        private String page;

        SpiderRun(String url, String key, String page) {
            this.url = url;
            this.key = key;
            this.page = page;
        }

        public void run() {
            try {
                Vector<String> t_urls = new Vector<String>();
                Connection conn = Jsoup.connect(url);
                HashMap<String, String> header = new HashMap<String, String>();
                header.put("Accept", "text/html, */*; q=0.01");
                header.put("Accept-Encoding", "gzip, deflate, br");
                header.put("Accept-Language", "zh-CN,zh;q=0.9");
                header.put("Connection", "keep-alive");
                header.put("Content-Length", "687");
                header.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                header.put("Cookie", "ASP.NET_SessionId=bxn31f0f2wc5iyepwniwwciv; Ecp_ClientId=7210201145500775087; SID_kcms=124109; Ecp_IpLoginFail=210201218.94.67.78; Ecp_ClientIp=218.94.67.78; SID_kns_new=kns123118; SID_kns8=123110; cnkiUserKey=9b8fee49-4d5c-7706-1bfb-e8c8ea873684; CurrSortFieldType=desc; _pk_ses=*; knsLeftGroupSelectItem=; ASPSESSIONIDSCCRARTT=OKKODMDDINKMJAFKLAKFLHDN; CurrSortField=%e5%b9%b4%2f(%e5%b9%b4%2c%27DATE%27); _pk_id=5d4253fa-08c1-4055-9b2d-51f68577700f.1612162585.1.1612166557.1612162585.");
                header.put("Host", "kns.cnki.net");
                header.put("Origin", "https://kns.cnki.net");
                header.put("Referer", "https://kns.cnki.net/kns8/defaultresult/index");
                header.put("sec-ch-ua", "\"Google Chrome\";v=\"87\", \" Not;A Brand\";v=\"99\", \"Chromium\";v=\"87\"");
                header.put("sec-ch-ua-mobile", "?0");
                header.put("Sec-Fetch-Dest", "empty");
                header.put("Sec-Fetch-Mode", "cors");
                header.put("Sec-Fetch-Site", "same-origin");
                header.put("X-Requested-With", "XMLHttpRequest");
                header.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
                Connection headersConn = conn.headers(header);
                HashMap<String, String> formData = new HashMap<String, String>();
                formData.put("IsSearch", "true");
                formData.put("QueryJson", "{\"Platform\":\"\",\"DBCode\":\"SNAD\",\"KuaKuCode\":\"\",\"QNode\":{\"QGroup\":[{\"Key\":\"Subject\",\"Title\":\"\",\"Logic\":1,\"Items\":[{\"Title\":\"主题\",\"Name\":\"SU\",\"Value\":\"" + key + "\",\"Operate\":\"%=\",\"BlurType\":\"\"}],\"ChildItems\":[]}]}}");
                formData.put("PageName", "DefaultResult");
                formData.put("DBCode", "SNAD");
                formData.put("KuaKuCodes", "");
                formData.put("CurPage", page);
                formData.put("RecordsCntPerPage", "20");
                formData.put("CurDisplayMode", "listmode");
                formData.put("CurrSortField", "%e5%b9%b4%2f(%e5%b9%b4%2c%27DATE%27)");
                formData.put("CurrSortFieldType", "desc");
                formData.put("IsSentenceSearch", "false");
                formData.put("Subject", "");

                Connection dataConn = headersConn.data(formData);
                Document doc = dataConn.post();
                Elements elements = doc.getElementsByClass("fz14");

                ConcurrentHashMap<String, ArrayList<String>> threadtitlrLinkedContent = new ConcurrentHashMap<String, ArrayList<String>>();
                insertKeyAndValues(elements, threadtitlrLinkedContent);

                //将线程的集合数据写入共享变量
                titleLinkContent.putAll(threadtitlrLinkedContent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void insertKeyAndValues(Elements elements, ConcurrentHashMap<String, ArrayList<String>> titleLinkContent) throws IOException {
            for (Element element : elements) {
                final String title = element.text();
                //添加对应的连接
                Elements linkElements = element.select("a");
                String[] filename = linkElements.attr("href").split("filename=");//获取filename
                String url = "https://kns.cnki.net/kcms/detail/detail.aspx?dbcode=SNAD&dbname=SNAD&filename=" + filename[1];
                //添加标题 键为url地址，值1为标题 值2为成果简介
                titleLinkContent.put(url, new ArrayList<String>() {{
                    add(title);
                }});
            }
            Set<Map.Entry<String, ArrayList<String>>> entries = titleLinkContent.entrySet();
            for (Map.Entry<String, ArrayList<String>> entry : entries) {
                //获取url并爬取
                String t_url = entry.getKey();
                Document doc2 = Jsoup.connect(t_url).get();
                Elements brief = doc2.getElementsByClass("brief");
                Elements rowtit = doc2.getElementsByClass("rowtit");
                Elements funds = brief.get(0).getElementsByClass("funds");
                for (int i = 0; i < rowtit.size(); i++) {
                    if ("成果简介：".equals(rowtit.get(i).text())) {
                        //添加成果简介
                        String text = funds.get(i).text();
                        ArrayList<String> titleAndContent = titleLinkContent.get(t_url);
                        titleAndContent.add(text);
                        titleLinkContent.put(t_url, titleAndContent);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        titleLinkContent = new ConcurrentHashMap<String, ArrayList<String>>();
        try {
            Scanner input = new Scanner(System.in);
            System.out.println("输入在知网搜索的关键词：");
            String keyword = input.next();
            System.out.println("输入想要查找的前几页（如果输入2，即找前2页的）：");
            String pagenumber = input.next();
            String txt_name = "关键词：" + keyword + "前" + pagenumber + "页具体内容.txt";
            File file = new File(txt_name);
            int pagenum = Integer.parseInt(pagenumber);
            long spiderStartTime = System.currentTimeMillis();
            for (int i = 1; i <= pagenum; i++) {
                String page = Integer.toString(i);
                String url = "https://kns.cnki.net/KNS8/Brief/GetGridTableHtml";
                pool.execute(new SpiderRun(url, keyword, page));
            }
            pool.shutdown();
            while (true) {
                if (pool.isTerminated()) {
                    break;
                } else {
                    System.out.println(Thread.activeCount());
                }
                Thread.sleep(1);
            }
            long spiderEndTime = System.currentTimeMillis();
            double spiderTime = (spiderEndTime - spiderStartTime) / 1000.0;
            System.out.println("爬虫运行耗时=======" + spiderTime + "秒============");
            long writeStartTime = System.currentTimeMillis();
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fileWriter);
            //写入文件
            Iterator<Map.Entry<String, ArrayList<String>>> it = titleLinkContent.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ArrayList<String>> next = it.next();
                bw.write(next.getValue().get(0));//第一个元素是title 第二个元素是成果摘要
                bw.write("\n");
                bw.write(next.getValue().get(1));
                bw.write("\n\n");
            }
            bw.close();
            long writerEndTime = System.currentTimeMillis();
            double writeTime = (writerEndTime - writeStartTime) / 1000.0;
            System.out.println("写文件耗时：===========" + writeTime + "秒==============");
            System.out.println("txt文件已经成功记录！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
