/*  Copyright (c) 2010 Xiaoyun Zhu
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining a copy  
 *  of this software and associated documentation files (the "Software"), to deal  
 *  in the Software without restriction, including without limitation the rights  
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell  
 *  copies of the Software, and to permit persons to whom the Software is  
 *  furnished to do so, subject to the following conditions:
 *  
 *  The above copyright notice and this permission notice shall be included in  
 *  all copies or substantial portions of the Software.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR  
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,  
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER  
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,  
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN  
 *  THE SOFTWARE.  
 */
package cn.kk.kkdict.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import cn.kk.kkdict.Configuration;
import cn.kk.kkdict.Configuration.Source;
import cn.kk.kkdict.beans.DictByteBufferRow;
import cn.kk.kkdict.extraction.dict.WikiPagesMetaCurrentExtractor;
import cn.kk.kkdict.types.Language;
import cn.kk.kkdict.utils.ArrayHelper;
import cn.kk.kkdict.utils.Helper;

/**
 * 输出所有含有某个语言的行
 */
public class DictFilesExtractor {
  private final String[]          inFiles;
  private final String            outDir;
  private final Language          extractLng;
  public static final String      SUFFIX_SKIPPED = "_xtr-skipped";
  public static final String      OUTDIR         = WikiPagesMetaCurrentExtractor.OUT_DIR;
  public static final String      OUTFILE        = "output-dict_xtr-result.wiki";
  private static final boolean    DEBUG          = false;
  public final String             outFile;
  private final ByteBuffer        lngBB;
  private boolean                 writeSkipped;
  private final DictByteBufferRow mainRow        = new DictByteBufferRow();

  @SuppressWarnings("unused")
  public static void main(final String[] args) throws IOException {
    final String outDir = Configuration.IMPORTER_FOLDER_EXTRACTED_DICTS.getPath(Source.DICT_WIKIPEDIA);
    final String inFileTest = outDir + File.separator + "output-dict.wiki_ang";
    // String inFileTest = "O:\\kkdict\\out\\dicts\\wiki\\test\\test.txt";
    final String inFile0 = "O:\\kkdict\\out\\dicts\\wiki\\test\\output-dict_ar.wiki_ar";
    final String inFile1 = "O:\\kkdict\\out\\dicts\\wiki\\test\\output-dict_bg.wiki_bg";
    final String inFile2 = "O:\\kkdict\\out\\dicts\\wiki\\test\\output-dict_be.wiki_be";
    final String inFile3 = "O:\\kkdict\\out\\dicts\\wiki\\test\\output-dict_az.wiki_az";
    final String inFile4 = "O:\\kkdict\\out\\dicts\\wiki\\test\\output-dict_bs.wiki_bs";
    final String inFile5 = "O:\\kkdict\\out\\dicts\\wiki\\test\\output-dict_br.wiki_br";
    final String inFile6 = "O:\\kkdict\\out\\dicts\\wiki\\test\\output-dict_an.wiki_an";
    final String inFile7 = "O:\\kkdict\\out\\dicts\\wiki\\test\\output-dict_af.wiki_af";
    final String inFile8 = "O:\\kkdict\\out\\dicts\\wiki\\test\\output-dict_bn.wiki_bn";
    final String inFile9 = "O:\\kkdict\\out\\dicts\\wiki\\test\\output-dict_ast.wiki_ast";
    // String inFile1 = "D:\\test1.txt";
    // String inFile2 = "D:\\test2.txt";
    // String inFile3 = "D:\\test3.txt";

    new DictFilesExtractor(Language.ZH, outDir, DictFilesExtractor.OUTFILE, true, inFileTest).extract();
    // new DictFilesExtractor(Language.ZH, outDir, OUTFILE, false, inFile0, inFile1, inFile2, inFile3, inFile4,
    // inFile5, inFile6, inFile7, inFile8, inFile9).extract();
  }

  public DictFilesExtractor(final Language extractLng, final String outDir, final String outFileName, final boolean writeSkipped, final String... inFiles) {
    if (new File(outDir).isDirectory()) {
      this.inFiles = inFiles;
      this.outDir = outDir;
      this.extractLng = extractLng;
      this.outFile = outDir + File.separator + outFileName;
      this.lngBB = ByteBuffer.wrap(extractLng.getKeyBytes());
      this.writeSkipped = writeSkipped;
    } else {
      this.inFiles = null;
      this.outDir = null;
      this.extractLng = null;
      this.outFile = null;
      this.lngBB = null;
      System.err.println("文件夹不可读：'" + outDir + "'!");
    }
  }

  public void extract() throws IOException {
    System.out.println("截取含有'" + this.extractLng.getKey() + "'的词典   。。。" + this.inFiles.length);
    if (DictFilesExtractor.DEBUG) {
      System.out.println("创建输出文件'" + this.outFile + "'。。。");
    }
    try (final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(this.outFile), Helper.BUFFER_SIZE);) {
      for (final String inFile : this.inFiles) {
        final File f = new File(inFile);
        if (f.isFile()) {
          if (DictFilesExtractor.DEBUG) {
            System.out.println("处理截取文件'" + f.getAbsolutePath() + "'（" + Helper.formatSpace(f.length()) + "）。。。");
          }
          String skippedOutFile = null;
          if (this.writeSkipped) {
            skippedOutFile = Helper.appendFileName(this.outDir + File.separator + f.getName(), DictFilesExtractor.SUFFIX_SKIPPED);
          }
          this.extract(out, f, skippedOutFile);
          if (DictFilesExtractor.DEBUG) {
            System.out.println("处理wiki文件成功：'" + f.getAbsolutePath() + "'，不符合条件文件：'" + skippedOutFile + "'（"
                + Helper.formatSpace(new File(skippedOutFile).length()) + "）");
          }
        } else {
          System.err.println("wiki文件不可读'" + f.getAbsolutePath() + "'！");
        }
      }
    }
    System.out.println("截取成功'" + this.outFile + "'（" + Helper.formatSpace(new File(this.outFile).length()) + "）。");

  }

  @SuppressWarnings("resource")
  private void extract(final BufferedOutputStream out, final File f, final String skippedOutFile) throws IOException {
    BufferedOutputStream skippedOut = null;
    if (skippedOutFile != null) {
      skippedOut = new BufferedOutputStream(new FileOutputStream(skippedOutFile), Helper.BUFFER_SIZE);
    }
    try (final BufferedInputStream in = new BufferedInputStream(new FileInputStream(f), Helper.BUFFER_SIZE);) {
      final ByteBuffer bb = ArrayHelper.borrowByteBufferMedium();
      final byte[] array = bb.array();
      int limit;
      while (-1 != ArrayHelper.readLine(in, bb)) {
        limit = bb.limit();
        this.mainRow.parseFrom(bb);
        if (-1 != this.mainRow.indexOfLanguage(this.lngBB)) {
          out.write(array, 0, limit);
          out.write(Helper.SEP_NEWLINE_CHAR);
        } else if (skippedOut != null) {
          skippedOut.write(array, 0, limit);
          skippedOut.write(Helper.SEP_NEWLINE_CHAR);
        }
      }
      if (skippedOut != null) {
        skippedOut.close();
      }
      ArrayHelper.giveBack(bb);
    }
  }
}
