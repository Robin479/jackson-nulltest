package com.fasterxml.jackson.databind;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.type.SimpleType;

/**
 * This test class has been written against the 2.9 code base of Jackson,
 * observing and conserving the implemented behavior of some of the methods in
 * the {@link ObjectMapper} class, that have been equipped with eager non-null
 * assertions in the 2.10 code base (see
 * <a href="https://github.com/FasterXML/jackson-databind/pull/2348">here</a>).
 * <p>
 * This aforementioned change has lead to a change in behavior for most effected
 * methods, which can be observed by successfully running these tests against
 * the 2.9 code base and then re-running and failing them against the 2.10 code
 * base.
 * <p>
 * All tests have been tagged to easily identify the breaking change.
 * <p>
 * Methods, that don't have an explicit documentation, have nothing to say
 * about, except that their behavior regarding {@code null} input changed from
 * the 2.9 code base to the 2.10 code base according to their respective tag.
 * 
 * @author <a href="mailto:kai@ooch.de">Kai Kunstmann</a>
 */

public class ObjectMapperNullabilityTest {
	/**
	 * Indicates a test, where the method's behavior for a {@code null} input
	 * prior to the non-null assertion was to throw a
	 * {@link NullPointerException} instead of an
	 * {@link IllegalArgumentException}. Hence, a pre-existing client would
	 * expect a {@link NullPointerException}, if anything.
	 * <p>
	 * IMHO, this might be only a minor change and could be acceptable, because
	 * both exceptions are {@link RuntimeException}s. However, it is a breaking
	 * change for some clients. Also, IMHO, an illegal {@code null} pointer
	 * should be punished with either a {@link NullPointerException} (for what
	 * it is) or should return a {@code null} result (where it makes sense, for
	 * letting the caller handle it). In either case, the implemented change of
	 * throwing an {@link IllegalArgumentException} for a {@code null} input is
	 * non-intuitive, but this is of course debatable.
	 */
	private static final String EXPECT_NPE = "expect-NPE";
	
	/**
	 * Indicates a test, where the method's behavior for a {@code null} input
	 * prior to the non-null assertion was to throw some kind of checked
	 * {@link JsonProcessingException} instead of an
	 * {@link IllegalArgumentException}. Hence, a pre-existing client would
	 * expect a {@link JsonProcessingException} or even {@link IOException}.
	 * <p>
	 * IMHO, this change is major, because most users would only expect to catch
	 * {@link JsonProcessingException}s and/or {@link IOException}s. Exceptions
	 * for {@code null} input that used to be checked
	 * {@link JsonProcessingException} in the 2.9 code base are unchecked
	 * {@link IllegalArgumentException}s in the 2.10 code base, which most users
	 * are most likely not handling, because they simply did not occur
	 * previously. This is definitively a client breaking change. The previous
	 * reasoning about returning {@code null} or throwing
	 * {@link NullPointerException}s (rather than
	 * {@link IllegalArgumentException}s) applies, as well.
	 */
	private static final String EXPECT_JPE = "expect-JPE";
	
	/**
	 * Indicates a test, where the method's behavior for a {@code null} input
	 * prior to the non-null assertion was to handle the {@code null} reference
	 * silently, by returning a sensible result (if necessary, e.g. {@code null}
	 * or an empty {@link Iterator}) instead of throwing an
	 * {@link IllegalArgumentException}. Hence, a pre-existing client would
	 * expect no exception at all.
	 * <p>
	 * IMHO, this change is major, because input that used to be <i>valid</i> is
	 * now considered illegal, and the indicating exception thrown for this anew
	 * alleged violation is merely an unchecked
	 * {@link IllegalArgumentException}, which most users are most likely not
	 * handling, because this simply did not occur previously. If anything,
	 * users will most likely only expect a checked
	 * {@link JsonProcessingException} (see above).
	 * <p>
	 * It is debatable, whether the methods in question should silently handle
	 * {@code null} or throw an exceptions:
	 * <ul>
	 * <li>Deleting a configuration object (as in the first 2 cases) should
	 * certainly not be illegal. If a user wants to do such a thing, she should
	 * be allowed to do so. The {@link ObjectMapper} may not function properly
	 * without a configuration, but that is the user's own fault. IMHO, I don't
	 * mind the exception, but I consider it unnecessary and misplaced.</li>
	 * <li>Reading from a {@code null} input source (as in the last 3 cases)
	 * should certainly rather throw an exception than silently accept the
	 * {@code null} input, because it is an error on the caller's side. In the
	 * 2.9 version, those 3 methods returned {@code null} (and an empty
	 * {@link Iterator}, respectively) instead of throwing such an exception. In
	 * the 2.10 version, those 3 methods throw an exception indicating the input
	 * violation. However, as mentioned earlier, those exceptions are
	 * non-intuitive {@link IllegalArgumentException}s rather than expected
	 * checked {@link JsonProcessingException}s or intuitive
	 * {@link NullPointerException}s.</li>
	 * </ul>
	 */
	private static final String UNEXPECTED = "unexpected";
	
	/**
	 * Indicates a test, where the method's behavior for a {@code null} input is
	 * somehow inconsistent in either the 2.9 code base or the newer 2.10 code
	 * base. A detailed explanation should be given in the comment.
	 * <p>
	 * The positive aspect here is, that most of the inconsistencies refer to
	 * the 2.9 code base, and have mostly been straightened by consistently
	 * throwing {@link IllegalArgumentException}s for {@code null} input
	 * (although, IMHO, it is not the proper exception to be thrown).
	 */
	private static final String INCONSISTENT = "inconsistent";
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void registerModule_Module() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.registerModule((Module) null);
		}, "expected plain NullPointerException");
	}
	
	/**
	 * INCONSISTENCY: With null-assertions implemented in 2.10
	 * {@link ObjectMapper#registerModules(Module...)} still throws a
	 * {@link NullPointerException}, which is inconsistent with all other
	 * methods (especially {@link ObjectMapper#registerModule(Module)} and
	 * {@link ObjectMapper#registerModules(Iterable)}) where
	 * {@link NullPointerException}s have been turned into
	 * {@link IllegalArgumentException}s by the implemented non-null assertions.
	 * The method in question is simply missing that same assertion.
	 */
	@Test
	@Tag(ObjectMapperNullabilityTest.INCONSISTENT)
	public void registerModules_Modules() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.registerModules((Module[]) null);
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void registerModules_Iterable() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.registerModules((Iterable<Module>) null);
		}, "expected plain NullPointerException");
	}
	
	/**
	 * INCONSISTENCY: IMHO, even though the behavior of the method in question
	 * did not change, it is flawed, none-the-less. Albeit, it was already
	 * flawed in the 2.9 version. It should either return {@code null} (for
	 * leniency and simplicity, letting the caller handle a {@code null} result
	 * on a {@code null} input) or throw a {@link NullPointerException} (for
	 * what it is), or throw a {@link JsonProcessingException} (following other
	 * methods). The inconsistency here is, however, the fact that the method
	 * already threw an {@link IllegalArgumentException} in the 2.9 version,
	 * where all other methods would have thrown a {@link NullPointerException}s
	 * or {@link JsonProcessingException}s. It is the only method in the 2.9
	 * code base to have done this.
	 */
	@Test
	@Tag(ObjectMapperNullabilityTest.INCONSISTENT)
	public void constructType_Class() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			mapper.constructType(null);
		}, "expected IllegalArgumentException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.UNEXPECTED)
	public void setConfig_SerializationConfig() {
		final ObjectMapper mapper = new ObjectMapper();
		
		try {
			mapper.setConfig((SerializationConfig) null);
		} catch (final Throwable throwable) {
			Assertions.fail("deleting config should work", throwable);
		}
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.UNEXPECTED)
	public void setConfig_DeserializationConfig() {
		final ObjectMapper mapper = new ObjectMapper();
		
		try {
			mapper.setConfig((DeserializationConfig) null);
		} catch (final Throwable throwable) {
			Assertions.fail("deleting config should work", throwable);
		}
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_JsonParser_Class() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((JsonParser) null, Object.class);
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_JsonParser_TypeReference() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((JsonParser) null, new TypeReference<Object>() {
			});
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_JsonParser_ResolvedType() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((JsonParser) null, (ResolvedType) SimpleType.constructUnsafe(Object.class));
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_JsonParser_JavaType() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((JsonParser) null, (JavaType) SimpleType.constructUnsafe(Object.class));
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readTree_JsonParser() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readTree((JsonParser) null);
		}, "expected plain NullPointerException");
	}
	
	/**
	 * INCONSISTENCY: {@link ObjectMapper#readValues(JsonParser, JavaType)} and
	 * all overloaded sibling methods behave the same in the 2.9 version, in
	 * that they return an empty {@link Iterator} on a {@code null} parser. This
	 * is consistent among those siblings, but is inconsistent with the singular
	 * version {@link ObjectMapper#readValue(JsonParser, JavaType)} and the
	 * siblings thereof, which throw a {@link NullPointerException} on a
	 * {@code null} parser. Also, IMHO, turning a missing parser into an empty
	 * iterator feels very wrong.
	 */
	@Test
	@Tag(ObjectMapperNullabilityTest.UNEXPECTED)
	@Tag(ObjectMapperNullabilityTest.INCONSISTENT)
	public void readValues_JsonParser_JavaType() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		
		try {
			final JavaType type = SimpleType.constructUnsafe(Object.class);
			
			Assertions.assertNotNull(mapper.readValues((JsonParser) null, type), "expected empty Iterator");
		} catch (final AssertionError error) {
			throw error;
		} catch (final Throwable throwable) {
			Assertions.fail("unexpected exception", throwable);
		}
	}
	
	/**
	 * INCONSISTENCY: All other overloaded <i>readTree</i> methods throw a
	 * {@link NullPointerException} on a {@code null} input in the 2.9 code
	 * base, but this one returns a lenient {@code null}.
	 */
	@Test
	@Tag(ObjectMapperNullabilityTest.UNEXPECTED)
	@Tag(ObjectMapperNullabilityTest.INCONSISTENT)
	public void readTree_InputStream() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		
		try {
			Assertions.assertNull(mapper.readTree((InputStream) null), "expected null result from null InputStream");
		} catch (final AssertionError error) {
			throw error;
		} catch (final Throwable throwable) {
			Assertions.fail("unexpected exception", throwable);
		}
	}
	
	/**
	 * INCONSISTENCY: All other overloaded <i>readTree</i> methods throw a
	 * {@link NullPointerException} on a {@code null} input in the 2.9 code
	 * base, but this one returns a lenient {@code null}.
	 */
	@Test
	@Tag(ObjectMapperNullabilityTest.UNEXPECTED)
	@Tag(ObjectMapperNullabilityTest.INCONSISTENT)
	public void readTree_Reader() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		
		try {
			Assertions.assertNull(mapper.readTree((Reader) null), "expected null result from null Reader");
		} catch (final AssertionError error) {
			throw error;
		} catch (final Throwable throwable) {
			Assertions.fail("unexpected exception", throwable);
		}
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readTree_String() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readTree((String) null);
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readTree_Bytes() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readTree((byte[]) null);
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readTree_File() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readTree((File) null);
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readTree_URL() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readTree((URL) null);
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_JPE)
	public void writeValue_JsonGenerator_Object() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(JsonMappingException.class, () -> {
			mapper.writeValue((JsonGenerator) null, (Object) null);
		}, "expected JsonMappingException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_JPE)
	public void writeTree_JsonGenerator_TreeNode() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(JsonMappingException.class, () -> {
			mapper.writeTree((JsonGenerator) null, (TreeNode) null);
		}, "expected JsonMappingException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_JPE)
	public void writeTree_JsonGenerator_JsonNode() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(JsonMappingException.class, () -> {
			mapper.writeTree((JsonGenerator) null, (JsonNode) null);
		}, "expected JsonMappingException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void treeAsTokens_TreeNode() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.treeAsTokens((TreeNode) null);
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_File_Class() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((File) null, Object.class);
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_File_TypeReference() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((File) null, new TypeReference<Object>() {
			});
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_File_JavaType() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((File) null, (JavaType) SimpleType.constructUnsafe(Object.class));
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_URL_Class() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((URL) null, Object.class);
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_URL_TypeReference() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((URL) null, new TypeReference<Object>() {
			});
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_URL_JavaType() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((URL) null, (JavaType) SimpleType.constructUnsafe(Object.class));
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_String_Class() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((String) null, Object.class);
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_String_TypeReference() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((String) null, new TypeReference<Object>() {
			});
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_String_JavaType() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((URL) null, (JavaType) SimpleType.constructUnsafe(Object.class));
		}, "expected plain NullPointerException");
	}
	
	/**
	 * INCONSISTENCY: Most sibling methods <i>readValue</i> as well as all
	 * <i>writeValue</i> methods throw a {@link NullPointerException} on a
	 * {@code null} input in the 2.9 code base, but this one throws a
	 * {@link MismatchedInputException}.
	 */
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_JPE)
	@Tag(ObjectMapperNullabilityTest.INCONSISTENT)
	public void readValue_Reader_Class() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(MismatchedInputException.class, () -> {
			mapper.readValue((Reader) null, Object.class);
		}, "expected MismatchedInputException");
	}
	
	/**
	 * INCONSISTENCY: Most sibling methods <i>readValue</i> as well as all
	 * <i>writeValue</i> methods throw a {@link NullPointerException} on a
	 * {@code null} input in the 2.9 code base, but this one throws a
	 * {@link MismatchedInputException}.
	 */
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_JPE)
	@Tag(ObjectMapperNullabilityTest.INCONSISTENT)
	public void readValue_Reader_TypeReference() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(MismatchedInputException.class, () -> {
			mapper.readValue((Reader) null, new TypeReference<Object>() {
			});
		}, "expected MismatchedInputException");
	}
	
	/**
	 * INCONSISTENCY: Most sibling methods <i>readValue</i> as well as all
	 * <i>writeValue</i> methods throw a {@link NullPointerException} on a
	 * {@code null} input in the 2.9 code base, but this one throws a
	 * {@link MismatchedInputException}.
	 */
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_JPE)
	@Tag(ObjectMapperNullabilityTest.INCONSISTENT)
	public void readValue_Reader_JavaType() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(MismatchedInputException.class, () -> {
			mapper.readValue((Reader) null, (JavaType) SimpleType.constructUnsafe(Object.class));
		}, "expected MismatchedInputException");
	}
	
	/**
	 * INCONSISTENCY: Most sibling methods <i>readValue</i> as well as all
	 * <i>writeValue</i> methods throw a {@link NullPointerException} on a
	 * {@code null} input in the 2.9 code base, but this one throws a
	 * {@link MismatchedInputException}.
	 */
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_JPE)
	@Tag(ObjectMapperNullabilityTest.INCONSISTENT)
	public void readValue_InputStream_Class() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(MismatchedInputException.class, () -> {
			mapper.readValue((InputStream) null, Object.class);
		}, "expected MismatchedInputException");
	}
	
	/**
	 * INCONSISTENCY: Most sibling methods <i>readValue</i> as well as all
	 * <i>writeValue</i> methods throw a {@link NullPointerException} on a
	 * {@code null} input in the 2.9 code base, but this one throws a
	 * {@link MismatchedInputException}.
	 */
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_JPE)
	@Tag(ObjectMapperNullabilityTest.INCONSISTENT)
	public void readValue_InputStream_TypeReference() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(MismatchedInputException.class, () -> {
			mapper.readValue((InputStream) null, new TypeReference<Object>() {
			});
		}, "expected MismatchedInputException");
	}
	
	/**
	 * INCONSISTENCY: Most sibling methods <i>readValue</i> as well as all
	 * <i>writeValue</i> methods throw a {@link NullPointerException} on a
	 * {@code null} input in the 2.9 code base, but this one throws a
	 * {@link MismatchedInputException}.
	 */
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_JPE)
	@Tag(ObjectMapperNullabilityTest.INCONSISTENT)
	public void readValue_InputStream_JavaType() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(MismatchedInputException.class, () -> {
			mapper.readValue((InputStream) null, (JavaType) SimpleType.constructUnsafe(Object.class));
		}, "expected MismatchedInputException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_Bytes_Class() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((byte[]) null, Object.class);
		}, "expected plain NullPointerException");
	}
	
	/**
	 * INCONSISTENCY: The method
	 * {@link ObjectMapper#readValue(byte[], int, int, Class)} throws a
	 * {@link MismatchedInputException} in the 2.9 code base, while its sibling
	 * method {@link ObjectMapper#readValue(byte[], Class)} and most other
	 * <i>readValue</i> methods throw a {@link NullPointerException} for a
	 * {@code null} input.
	 */
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_JPE)
	@Tag(ObjectMapperNullabilityTest.INCONSISTENT)
	public void readValue_Bytes_int_int_Class() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(MismatchedInputException.class, () -> {
			mapper.readValue((byte[]) null, 0, 0, Object.class);
		}, "expected MismatchedInputException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_Bytes_TypeReference() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((byte[]) null, new TypeReference<Object>() {
			});
		}, "expected plain NullPointerException");
	}
	
	/**
	 * INCONSISTENCY: The method
	 * {@link ObjectMapper#readValue(byte[], int, int, TypeReference)} throws a
	 * {@link MismatchedInputException} in the 2.9 code base, while its sibling
	 * method {@link ObjectMapper#readValue(byte[], TypeReference)} and most
	 * other <i>readValue</i> methods throw a {@link NullPointerException} for a
	 * {@code null} input.
	 */
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_JPE)
	@Tag(ObjectMapperNullabilityTest.INCONSISTENT)
	public void readValue_Bytes_int_int_TypeReference() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(MismatchedInputException.class, () -> {
			mapper.readValue((byte[]) null, 0, 0, new TypeReference<Object>() {
			});
		}, "expected MismatchedInputException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_Bytes_JavaType() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((byte[]) null, (JavaType) SimpleType.constructUnsafe(Object.class));
		}, "expected plain NullPointerException");
	}
	
	/**
	 * INCONSISTENCY: The method
	 * {@link ObjectMapper#readValue(byte[], int, int, JavaType)} throws a
	 * {@link MismatchedInputException} in the 2.9 code base, while its sibling
	 * method {@link ObjectMapper#readValue(byte[], JavaType)} and most other
	 * <i>readValue</i> methods throw a {@link NullPointerException} for a
	 * {@code null} input.
	 */
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_JPE)
	@Tag(ObjectMapperNullabilityTest.INCONSISTENT)
	public void readValue_Bytes_int_int_JavaType() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(MismatchedInputException.class, () -> {
			mapper.readValue((byte[]) null, 0, 0, (JavaType) SimpleType.constructUnsafe(Object.class));
		}, "expected MismatchedInputException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_DataInput_Class() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((DataInput) null, Object.class);
		}, "expected plain NullPointerException");
	}
	
//	/**
//	 * INCONSISTENCY: This method does not exist. ;)
//	 */
//	@Test
//	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
//	@Tag(ObjectMapperNullabilityTest.INCONSISTENT)
//	public void readValue_DataInput_TypeReference()
//	{
//		final ObjectMapper mapper = new ObjectMapper();
//
//		Assertions.assertThrows(NullPointerException.class, () ->
//		{
//			mapper.readValue((DataInput) null, new TypeReference<Object>()
//			{
//			});
//		}, "expected plain NullPointerException");
//	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void readValue_DataInput_JavaType() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.readValue((DataInput) null, (JavaType) SimpleType.constructUnsafe(Object.class));
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void writeValue_File_Object() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.writeValue((File) null, (Object) null);
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void writeValue_OutputStream_Object() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.writeValue((OutputStream) null, (Object) null);
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void writeValue_DataOutput_Object() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.writeValue((DataOutput) null, (Object) null);
		}, "expected plain NullPointerException");
	}
	
	@Test
	@Tag(ObjectMapperNullabilityTest.EXPECT_NPE)
	public void writeValue_Writer_Object() {
		final ObjectMapper mapper = new ObjectMapper();
		
		Assertions.assertThrows(NullPointerException.class, () -> {
			mapper.writeValue((Writer) null, (Object) null);
		}, "expected plain NullPointerException");
	}
}
