package ${configYAML.apiPackagePath}.dto.${versionDirName};

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.liferay.petra.function.UnsafeSupplier;
import com.liferay.petra.string.StringBundler;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.Date;

import javax.annotation.Generated;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author ${configYAML.author}
 * @generated
 */
@Generated("")
@GraphQLName("${schemaName}")
@XmlRootElement(name = "${schemaName}")
public class ${schemaName} {

	<#list freeMarkerTool.getDTOJavaParameters(configYAML, openAPIYAML, schema, false) as javaParameter>
		public ${javaParameter.parameterType} get${javaParameter.parameterName?cap_first}() {
			return ${javaParameter.parameterName};
		}

		public void set${javaParameter.parameterName?cap_first}(${javaParameter.parameterType} ${javaParameter.parameterName}) {
			this.${javaParameter.parameterName} = ${javaParameter.parameterName};
		}

		@JsonIgnore
		public void set${javaParameter.parameterName?cap_first}(UnsafeSupplier<${javaParameter.parameterType}, Exception> ${javaParameter.parameterName}UnsafeSupplier) {
			try {
				${javaParameter.parameterName} = ${javaParameter.parameterName}UnsafeSupplier.get();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@GraphQLField
		@JsonProperty
		protected ${javaParameter.parameterType} ${javaParameter.parameterName};
	</#list>

	public String toString() {
		StringBundler sb = new StringBundler();

		sb.append("{");

		<#list freeMarkerTool.getDTOJavaParameters(configYAML, openAPIYAML, schema, false) as javaParameter>
			<#if !javaParameter?is_first>
				sb.append(", ");
			</#if>

			sb.append("\"${javaParameter.parameterName}\": ");

			<#if stringUtil.equals(javaParameter.parameterType, "Date") || stringUtil.equals(javaParameter.parameterType, "String") || javaParameter.parameterType?contains("[]")>
				sb.append("\"");
				sb.append(${javaParameter.parameterName});
				sb.append("\"");
			<#else>
				sb.append(${javaParameter.parameterName});
			</#if>
		</#list>

		sb.append("}");

		return sb.toString();
	}

}