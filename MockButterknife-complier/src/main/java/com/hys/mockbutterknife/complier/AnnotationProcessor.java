package com.hys.mockbutterknife.complier;

import com.hys.mockbutterknife.annotations.BindView;
import com.hys.mockbutterknife.annotations.OnClick;
import com.hys.mockbutterknife.model.BindSet;
import com.squareup.javapoet.JavaFile;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.tools.Diagnostic;

/*
* Created by 胡延森(QQ:1015950695) on 2018/2/8
*/

public class AnnotationProcessor extends AbstractProcessor{

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportedAnnotationTypes = new LinkedHashSet<>();

        Iterator ite = getSupportedAnnotations().iterator();
        while (ite.hasNext()){
            Class annotation = (Class<? extends Annotation>)ite.next();
            supportedAnnotationTypes.add(annotation.getCanonicalName());
        }

        return supportedAnnotationTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<TypeElement, BindSet> bindMap = findAndParseTargets(roundEnv);

        Iterator ite = bindMap.entrySet().iterator();
        while(ite.hasNext()){
            Entry<TypeElement, BindSet> entry = (Entry)ite.next();
            TypeElement typeElement = entry.getKey();
            BindSet bindSet = entry.getValue();
            JavaFile javaFile = bindSet.brewJava();

            try{
                javaFile.writeTo(this.processingEnv.getFiler());
            }catch (IOException ex){
                this.error(typeElement, "Unable to write binding for type %s: %s", typeElement, ex.getMessage());
            }
        }

        return false;
    }

    private Map<TypeElement, BindSet> findAndParseTargets(RoundEnvironment env) {
        Map<TypeElement, BindSet> bindMap = new LinkedHashMap<>();

        findAndParseBindView(env, bindMap);
        findAndParseListener(env, bindMap);

        return bindMap;
    }

    private void findAndParseBindView(RoundEnvironment env, Map<TypeElement, BindSet> bindMap){

        Iterator ite = env.getElementsAnnotatedWith(BindView.class).iterator();
        while(ite.hasNext()){
            Element element = (Element) ite.next();

            if(isInaccessible(element, "field", BindView.class)
                    || isInWrongPackage(element, BindView.class))
                continue;

            TypeMirror elementType = element.asType();
            if(elementType.getKind() == TypeKind.TYPEVAR){
                TypeVariable typeVariable = (TypeVariable) elementType;
                elementType = typeVariable.getUpperBound();
            }

            if(!isSubtypeOfType(elementType, "android.view.View")
                    && !isInterface(elementType))
                continue;

            TypeElement encloseingElement = (TypeElement) element.getEnclosingElement();
            BindSet bindSet = bindMap.get(encloseingElement);
            if(bindSet == null){
                bindSet = new BindSet(encloseingElement, element, BindView.class);
                bindMap.put(encloseingElement, bindSet);
            }else {
                bindSet.addItem(element, BindView.class);
            }
        }
    }

    private void findAndParseListener(RoundEnvironment env, Map<TypeElement, BindSet> bindMap){

        Iterator ite = env.getElementsAnnotatedWith(OnClick.class).iterator();
        while(ite.hasNext()){
            Element element = (Element) ite.next();
            TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

            if(!(element instanceof ExecutableElement)
                    || element.getKind() != ElementKind.METHOD)
                continue;

            if(isInaccessible(element, "method", OnClick.class)
                    || isInWrongPackage(element, OnClick.class))
                continue;


            int[] ids = element.getAnnotation(OnClick.class).value();
            Integer duplicateId = findDuplicate(ids);
            if(duplicateId != null){
                this.error(element, "@%s annotation for method contains duplicate ID %d. (%s.%s)", OnClick.class.getSimpleName(), duplicateId, enclosingElement.getQualifiedName(), element.getSimpleName());
                continue;
            }

            List<? extends VariableElement> methodParameters = ((ExecutableElement)element).getParameters();
            if (methodParameters.size() <= 0 || methodParameters.size() > 1) {
                this.error(element, "@%s methods can have at most %s parameter(s). (%s.%s)", OnClick.class.getSimpleName(), 1, enclosingElement.getQualifiedName(), element.getSimpleName());
                continue;
            }

            TypeMirror parameterType = methodParameters.get(0).asType();
            if(!isSubtypeOfType(parameterType, "android.view.View")){
                this.error(element, "@%s fields must extend from View or be an interface. (%s.%s)", OnClick.class.getSimpleName(), enclosingElement.getQualifiedName(), element.getSimpleName());
                continue;
            }

            BindSet bindSet = bindMap.get(enclosingElement);
            if(bindSet == null){
                bindSet = new BindSet(enclosingElement, element, OnClick.class);
                bindMap.put(enclosingElement, bindSet);
            }else {
                bindSet.addItem(element, OnClick.class);
            }
        }
    }

    private boolean isInaccessible(Element element, String targetThing, Class<? extends Annotation> annotationClass) {

        TypeElement enclosingElement = (TypeElement)element.getEnclosingElement();
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.STATIC)) {
            this.error(element, "@%s %s must not be private or static. (%s.%s)", annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(), element.getSimpleName());
            return true;
        }

        if (enclosingElement.getKind() != ElementKind.CLASS) {
            this.error(enclosingElement, "@%s %s may only be contained in classes. (%s.%s)", annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(), element.getSimpleName());
            return true;
        }

        if (enclosingElement.getModifiers().contains(Modifier.PRIVATE)) {
            this.error(enclosingElement, "@%s %s may not be contained in private classes. (%s.%s)", annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(), element.getSimpleName());
            return true;
        }

        return false;
    }

    private boolean isInWrongPackage(Element element, Class<? extends Annotation> annotationClass) {

        TypeElement enclosingElement = (TypeElement)element.getEnclosingElement();
        String qualifiedName = enclosingElement.getQualifiedName().toString();
        if (qualifiedName.startsWith("android.")) {
            this.error(element, "@%s-annotated class incorrectly in Android framework package. (%s)", annotationClass.getSimpleName(), qualifiedName);
            return true;
        } else if (qualifiedName.startsWith("java.")) {
            this.error(element, "@%s-annotated class incorrectly in Java framework package. (%s)", annotationClass.getSimpleName(), qualifiedName);
            return true;
        }

        return false;
    }

    public static boolean isSubtypeOfType(TypeMirror typeMirror, String otherType) {
        if (isTypeEqual(typeMirror, otherType))
            return true;

        if (typeMirror.getKind() != TypeKind.DECLARED)
            return false;

        DeclaredType declaredType = (DeclaredType)typeMirror;
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() > 0) {
            StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
            typeString.append('<');

            for(int i = 0; i < typeArguments.size(); ++i) {
                if (i > 0) {
                    typeString.append(',');
                }

                typeString.append('?');
            }

            typeString.append('>');
            if (typeString.toString().equals(otherType)) {
                return true;
            }
        }

        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return false;
        } else {
            TypeElement typeElement = (TypeElement)element;
            TypeMirror superType = typeElement.getSuperclass();
            if (isSubtypeOfType(superType, otherType)) {
                return true;
            } else {
                Iterator var7 = typeElement.getInterfaces().iterator();

                TypeMirror interfaceType;
                do {
                    if (!var7.hasNext()) {
                        return false;
                    }

                    interfaceType = (TypeMirror)var7.next();
                } while(!isSubtypeOfType(interfaceType, otherType));

                return true;
            }
        }

    }

    private boolean isInterface(TypeMirror typeMirror) {
        if(!(typeMirror instanceof DeclaredType))
            return false;

        if(((DeclaredType) typeMirror).asElement().getKind() != ElementKind.INTERFACE)
            return false;

        return true;
    }

    private static boolean isTypeEqual(TypeMirror typeMirror, String otherType) {
        return otherType.equals(typeMirror.toString());
    }

    private static Integer findDuplicate(int[] array) {
        Set<Integer> destElements = new LinkedHashSet();
        int[] srcElements = array;
        int srcElementsLength = array.length;

        for(int index = 0; index < srcElementsLength; ++index) {
            int element = srcElements[index];
            if (!destElements.add(element)) {
                return element;
            }
        }

        return null;
    }

    private Set<Class<? extends Annotation>> getSupportedAnnotations(){
        Set<Class<? extends Annotation>> supportedAnnotations = new LinkedHashSet<>();
        supportedAnnotations.add(BindView.class);
        supportedAnnotations.add(OnClick.class);
        return supportedAnnotations;
    }

    private void error(Element element, String message, Object... args) {
        this.printMessage(Diagnostic.Kind.ERROR, element, message, args);
    }

    private void printMessage(Diagnostic.Kind kind, Element element, String message, Object[] args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }

        this.processingEnv.getMessager().printMessage(kind, message, element);
    }
}
