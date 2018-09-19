package com.cangwang.generate

import android.util.ArraySet
import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.io.FileUtils
import org.gradle.api.Project


/**遍历合成器
 * Created by cangwang on 2018/9/13.
 */
class GenerateTransform extends Transform{
    private static final String MainAddress = ".gradle/modulebus/main/"
    private static final String OutputAddress = ".gradle/modulebus/outputs/"

    Project project
    ArraySet<CtInfo> set = ArraySet()


    GenerateTransform(Project project) {    // 构造函数，我们将Project保存下来备用
        this.project = project
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        set.clear()
        def startTime = System.currentTimeMillis()

        File main = project.rootProject.file(MainAddress)
        if (!main.exists())
            main.mkdirs()

        File outputs = project.rootProject.file(OutputAddress)
        if (!outputs.exists())
            outputs.mkdirs()
        //以后需要添加过滤
        inputs.each {
            TransformInput input ->
                //先遍历jar
                try {
                    input.jarInputs.each {
                        set.addAll(GenerateUtil.getNeed(it.file.getAbsolutePath(),"com",project))

//                        String outputFileName = it.name.replace(".jar", "") + '-' + it.file.path.hashCode()
//                        def output = outputProvider.getContentLocation(outputFileName, it.contentTypes, it.scopes, Format.JAR)
//                        FileUtils.copyFile(it.file, output)
                    }
                } catch (Exception e) {
                    project.logger.err e.getMessage()
                }

                //对类型为“文件夹”的input进行遍历
                input.directoryInputs.each { DirectoryInput directoryInput ->
                    //文件夹里面包含的是我们手写的类以及R.class、BuildConfig.class以及R$XXX.class等
                    set.addAll(GenerateUtil.getNeed(it.file.getAbsolutePath(),"com",project))
                    // 获取output目录
//                    def dest = outputProvider.getContentLocation(directoryInput.name,
//                            directoryInput.contentTypes, directoryInput.scopes,
//                            Format.DIRECTORY)
//
//                    // 将input的目录复制到output指定目录
//                    FileUtils.copyDirectory(directoryInput.file, dest)

                }

                for (CtInfo info:set){
                    if (info.packageName!=null){
                        String address = info.packageName.replace(".","/")
                        File addressDir = project.rootProject.file(MainAddress+address)
                        if (!addressDir.exists())
                            addressDir.mkdirs()
                        def target = new File(addressDir,info.clazz.name)
                        FileUtils.copyFile(info.path,target)
                    }
                }

                if(project.plugins.findPlugin("com.android.application") //判断是Application module
                        && main.listFiles().length > 0 ){  //判断文件夹里面不为空
                    generateReleaseJar()
                }
        }
        project.logger.error("GenerateTransform cast :" + (System.currentTimeMillis() - startTime) / 1000 + " secs")
    }

    private static File generateReleaseJar(File classesDir, def argFiles, def classPath, def target, def source) {
        def classpathSeparator = ";"
        if (!System.properties['os.name'].toLowerCase().contains('windows')) {
            classpathSeparator = ":"
        }
        def p
        if (classPath.size() == 0) {  //解压
            p = ("javac -encoding UTF-8 -target " + target + " -source " + source + " -d . " + argFiles.join(' ')).execute(null, classesDir)
        } else {
            p = ("javac -encoding UTF-8 -target " + target + " -source " + source + " -d . -classpath " + classPath.join(classpathSeparator) + " " + argFiles.join(' ')).execute(null, classesDir)
        }

        def result = p.waitFor()
        if (result != 0) {
            throw new RuntimeException("Failure to convert java source to bytecode: \n" + p.err.text)
        }

        p = "jar cvf outputs/classes.jar -C classes . ".execute(null, classesDir.parentFile)  //读取java文件
        result = p.waitFor()
        p.destroy()
        p = null
        if (result != 0) {
            throw new RuntimeException("failure to package classes.jar: \n" + p.err.text)
        }

        return new File(classesDir.parentFile, 'outputs/classes.jar')  //返回classes.jar文件
    }

    @Override
    String getName() {
        return "___GenerateTransform___"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }
}