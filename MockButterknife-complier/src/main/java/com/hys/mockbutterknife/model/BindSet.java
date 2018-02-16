package com.hys.mockbutterknife.model;

import com.google.auto.common.MoreElements;
import com.hys.mockbutterknife.annotations.BindView;
import com.hys.mockbutterknife.annotations.OnClick;
import com.hys.mockbutterknife.complier.AnnotationProcessor;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Created by 胡延森(QQ:1015950695) on 2018/2/11.
 */

public class BindSet {

    private static final ClassName VIEW = ClassName.get("android.view", "View", new String[0]);
    private static final ClassName UI_THREAD = ClassName.get("android.support.annotation", "UiThread", new String[0]);

    private TypeElement encloseingElement;
    private Map<Element, Class<? extends Annotation>> itemMap = new LinkedHashMap<>();

    public BindSet(TypeElement encloseingElement, Element element, Class<? extends Annotation> annotationClass){

        this.encloseingElement = encloseingElement;

        if(null == itemMap.get(element))
            itemMap.put(element, annotationClass);
    }

    public JavaFile brewJava() {
        String packageName = MoreElements.getPackage(this.encloseingElement).getQualifiedName().toString();
        return JavaFile.builder(packageName, createTypeSpec()).build();
    }

    public void addItem(Element element, Class<? extends Annotation> annotationClass){
        this.itemMap.put(element, annotationClass);
    }

    private TypeSpec createTypeSpec(){
        String className = this.encloseingElement.getSimpleName().toString() + "_ViewBinding";
        TypeName targetTypeName = TypeName.get(this.encloseingElement.asType());

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(new Modifier[]{Modifier.PUBLIC})
                .addField(targetTypeName, "target", new Modifier[]{Modifier.PRIVATE});

        classBuilder.addFields(createFieldForListener());

        if(isActivity()){
            classBuilder.addMethod(createConstructorForActivity());
        } else if(isView()){
            classBuilder.addMethod(createConstructorForView());
        } else if(isDialog()){
            classBuilder.addMethod(createConstructorForDialog());
        }

        classBuilder.addMethod(createBindConstructor());

        return classBuilder.build();
    }

    private List<FieldSpec> createFieldForListener(){
        ArrayList<FieldSpec> fieldList = new ArrayList<>();
        ArrayList<Integer> idList = new ArrayList<>();

        Iterator ite = this.itemMap.entrySet().iterator();
        while (ite.hasNext()) {
            Entry<Element, Class<? extends Annotation>> entry = (Entry) ite.next();
            Element element = entry.getKey();
            Class<? extends Annotation> annotationClass = entry.getValue();

           if (annotationClass.equals(OnClick.class)) {
                int[] ids = element.getAnnotation(OnClick.class).value();
                for (int index = 0; index < ids.length; ++index) {
                    if(idList.contains(ids[index]))
                        continue;

                    idList.add(ids[index]);

                    FieldSpec viewFieldSpec = FieldSpec.builder(VIEW, "view" + ids[index], new Modifier[]{Modifier.PRIVATE}).build();
                    fieldList.add(viewFieldSpec);
                }
            }
        }

        return fieldList;
    }

    private MethodSpec createConstructorForActivity(){

        TypeName targetTypeName = TypeName.get(this.encloseingElement.asType());
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addAnnotation(UI_THREAD)
                .addModifiers(new Modifier[]{Modifier.PUBLIC})
                .addParameter(targetTypeName, "target", new Modifier[0])
                .addStatement("this(target, target.getWindow().getDecorView())", new Object[0]);

        return builder.build();
    }

    private MethodSpec createConstructorForView(){

        TypeName targetTypeName = TypeName.get(this.encloseingElement.asType());
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addAnnotation(UI_THREAD)
                .addModifiers(new Modifier[]{Modifier.PUBLIC})
                .addParameter(targetTypeName, "target", new Modifier[0])
                .addStatement("this(target, target)", new Object[0]);

        return builder.build();
    }

    private MethodSpec createConstructorForDialog(){

        TypeName targetTypeName = TypeName.get(this.encloseingElement.asType());
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addAnnotation(UI_THREAD)
                .addModifiers(new Modifier[]{Modifier.PUBLIC})
                .addParameter(targetTypeName, "target", new Modifier[0])
                .addStatement("this(target, target.getWindow().getDecorView())", new Object[0]);

        return builder.build();
    }

    private boolean isView(){

        TypeMirror targetTypeMirror = this.encloseingElement.asType();
        return AnnotationProcessor.isSubtypeOfType(targetTypeMirror, "android.view.View");
    }

    private boolean isActivity(){

        TypeMirror targetTypeMirror = this.encloseingElement.asType();
        return AnnotationProcessor.isSubtypeOfType(targetTypeMirror, "android.app.Activity");
    }

    private boolean isDialog(){

        TypeMirror targetTypeMirror = this.encloseingElement.asType();
        return AnnotationProcessor.isSubtypeOfType(targetTypeMirror, "android.app.Dialog");
    }

    private MethodSpec createBindConstructor(){

        TypeName targetTypeName = TypeName.get(this.encloseingElement.asType());
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(new Modifier[]{Modifier.PUBLIC})
                .addParameter(targetTypeName, "target", new Modifier[]{Modifier.FINAL})
                .addParameter(VIEW, "source", new Modifier[0])
                .addStatement("this.target = target");

        Iterator ite = this.itemMap.entrySet().iterator();
        while (ite.hasNext()) {
            Entry<Element, Class<? extends Annotation>> entry = (Entry) ite.next();
            Element element = entry.getKey();
            Class<? extends Annotation> annotationClass = entry.getValue();

            if (annotationClass.equals(BindView.class)) {
                builder.addStatement("this.target.$N = ($T)source.findViewById($L)", element.getSimpleName(), element.asType(), element.getAnnotation(BindView.class).value());
            }else if(annotationClass.equals(OnClick.class)){
                int[] ids = element.getAnnotation(OnClick.class).value();
                for(int index = 0; index < ids.length; ++index){

                    builder.addStatement("this.$N = source.findViewById($L)", "view" + ids[index], ids[index]);
                    builder.addCode("this.$N.setOnClickListener(new $T.OnClickListener() {\n" +
                            "   @Override\n" +
                            "   public void onClick(View v) {\n" +
                            "       target.$N(v);\n" +
                            "   }\n" +
                            "});\n", "view" + ids[index], VIEW, element.getSimpleName());
                }
            }
        }

        return builder.build();
    }
}
