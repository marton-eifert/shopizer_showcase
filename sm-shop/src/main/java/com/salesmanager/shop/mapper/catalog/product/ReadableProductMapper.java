package com.salesmanager.shop.mapper.catalog.product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.catalog.pricing.PricingService;
import com.salesmanager.core.model.catalog.category.Category;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.attribute.ProductAttribute;
import com.salesmanager.core.model.catalog.product.attribute.ProductOption;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionDescription;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionValue;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionValueDescription;
import com.salesmanager.core.model.catalog.product.availability.ProductAvailability;
import com.salesmanager.core.model.catalog.product.description.ProductDescription;
import com.salesmanager.core.model.catalog.product.image.ProductImage;
import com.salesmanager.core.model.catalog.product.price.FinalPrice;
import com.salesmanager.core.model.catalog.product.price.ProductPrice;
import com.salesmanager.core.model.catalog.product.price.ProductPriceDescription;
import com.salesmanager.core.model.catalog.product.variant.ProductVariant;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.mapper.Mapper;
import com.salesmanager.shop.mapper.catalog.ReadableCategoryMapper;
import com.salesmanager.shop.mapper.catalog.ReadableManufacturerMapper;
import com.salesmanager.shop.mapper.catalog.ReadableProductTypeMapper;
import com.salesmanager.shop.model.catalog.category.ReadableCategory;
import com.salesmanager.shop.model.catalog.manufacturer.ReadableManufacturer;
import com.salesmanager.shop.model.catalog.product.ReadableImage;
import com.salesmanager.shop.model.catalog.product.ReadableProduct;
import com.salesmanager.shop.model.catalog.product.ReadableProductPrice;
import com.salesmanager.shop.model.catalog.product.attribute.ReadableProductAttribute;
import com.salesmanager.shop.model.catalog.product.attribute.ReadableProductAttributeValue;
import com.salesmanager.shop.model.catalog.product.attribute.ReadableProductOption;
import com.salesmanager.shop.model.catalog.product.attribute.ReadableProductProperty;
import com.salesmanager.shop.model.catalog.product.attribute.ReadableProductPropertyValue;
import com.salesmanager.shop.model.catalog.product.attribute.api.ReadableProductOptionValue;
import com.salesmanager.shop.model.catalog.product.product.ProductSpecification;
import com.salesmanager.shop.model.catalog.product.product.variant.ReadableProductVariant;
import com.salesmanager.shop.model.catalog.product.type.ReadableProductType;
import com.salesmanager.shop.model.references.DimensionUnitOfMeasure;
import com.salesmanager.shop.model.references.WeightUnitOfMeasure;
import com.salesmanager.shop.store.api.exception.ConversionRuntimeException;
import com.salesmanager.shop.utils.DateUtil;
import com.salesmanager.shop.utils.ImageFilePath;

/**
 * Works for product v2 model
 * 
 * @author carlsamson
 *
 */
@Component
public class ReadableProductMapper implements Mapper<Product, ReadableProduct> {

	// uses code that is similar to ProductDefinition
	@Autowired
	@Qualifier("img")
	private ImageFilePath imageUtils;

	@Autowired
	private ReadableCategoryMapper readableCategoryMapper;

	@Autowired
	private ReadableProductTypeMapper readableProductTypeMapper;

	@Autowired
	private ReadableProductVariantMapper readableProductVariantMapper;

	@Autowired
	private ReadableManufacturerMapper readableManufacturerMapper;

	@Autowired
	private PricingService pricingService;

	@Override
	public ReadableProduct convert(Product source, MerchantStore store, Language language) {
		ReadableProduct product = new ReadableProduct();
		return this.merge(source, product, store, language);
	}

	@Override
	public ReadableProduct merge(Product source, ReadableProduct destination, MerchantStore store, Language language) {

		Validate.notNull(source, "Product cannot be null");
		Validate.notNull(destination, "Product destination cannot be null");


		// read only product values
		// will contain options
		TreeMap<Long, ReadableProductOption> selectableOptions = new TreeMap<Long, ReadableProductOption>();

		destination.setSku(source.getSku());
		destination.setRefSku(source.getRefSku());
		destination.setId(source.getId());
		destination.setDateAvailable(DateUtil.formatDate(source.getDateAvailable()));

		ProductDescription description = null;
		if (source.getDescriptions() != null && source.getDescriptions().size() > 0) {
			for (ProductDescription desc : source.getDescriptions()) {
				if (language != null && desc.getLanguage() != null
						&& desc.getLanguage().getId().intValue() == language.getId().intValue()) {
					description = desc;
					break;
				}
			}
		}
		destination.setId(source.getId());
		destination.setAvailable(source.isAvailable());
		destination.setProductShipeable(source.isProductShipeable());

		destination.setPreOrder(source.isPreOrder());
		destination.setRefSku(source.getRefSku());
		destination.setSortOrder(source.getSortOrder());

		if (source.getType() != null) {
			ReadableProductType readableType = readableProductTypeMapper.convert(source.getType(), store, language);
			destination.setType(readableType);
		}

		if (source.getDateAvailable() != null) {
			destination.setDateAvailable(DateUtil.formatDate(source.getDateAvailable()));
		}

		if (source.getAuditSection() != null) {
			destination.setCreationDate(DateUtil.formatDate(source.getAuditSection().getDateCreated()));
		}

		destination.setProductVirtual(source.getProductVirtual());

		if (source.getProductReviewCount() != null) {
			destination.setRatingCount(source.getProductReviewCount().intValue());
		}

		if (source.getManufacturer() != null) {
			ReadableManufacturer manufacturer = readableManufacturerMapper.convert(source.getManufacturer(), store,
					language);
			destination.setManufacturer(manufacturer);
		}

		// images
		Set<ProductImage> images = source.getImages();
		if (CollectionUtils.isNotEmpty(images)) {

			List<ReadableImage> imageList = images.stream().map(i -> this.convertImage(source, i, store))
					.collect(Collectors.toList());
			destination.setImages(imageList);
		}

		if (!CollectionUtils.isEmpty(source.getAttributes())) {

			Set<ProductAttribute> attributes = source.getAttributes();

			if (!CollectionUtils.isEmpty(attributes)) {

				for (ProductAttribute attribute : attributes) {
					ReadableProductOption opt = null;
					ReadableProductAttribute attr = null;
					ReadableProductProperty property = null;
					ReadableProductPropertyValue propertyValue = null;




/**********************************
 * CAST-Finding START #1 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `ReadableProductAttributeValue attrValue = new ReadableProductAttributeValue();` is most likely affected. - Reasoning: It involves object instantiation inside a loop, which is a resource-intensive operation. - Proposed solution: Move the instantiation outside the loop and reuse the same object for each iteration.
 * INSTRUCTION: Please follow the OUTLINE and conduct the proposed steps with the affected code.
 * STATUS: REVIEWED
 * CAST-Finding END #1
 **********************************/


					ReadableProductAttributeValue attrValue = new ReadableProductAttributeValue();

					ProductOptionValue optionValue = attribute.getProductOptionValue();

					// we need to set readonly attributes only
					if (attribute.getAttributeDisplayOnly()) {// read only attribute = property

						property = createProperty(attribute, language);



/**********************************
 * CAST-Finding START #2 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `ProductOptionValue optionValue = attribute.getProductOptionValue();` is most likely affected. - Reasoning: It is inside the code section where the finding is located. - Proposed solution: Move the instantiation of `optionValue` outside the loop if possible, to avoid unnecessary instantiations.  The code line `ReadableProductOption readableOption = new ReadableProductOption();` is most likely affected. - Reasoning: It is inside the code section where the finding is located. - Proposed solution: Move the instantiation of `readableOption` outside the loop if possible, to avoid unnecessary instantiations.
 * INSTRUCTION: Please follow the OUTLINE and conduct the proposed steps with the affected code.
 * STATUS: REVIEWED
 * CAST-Finding END #2
 **********************************/
 **********************************/
 **********************************/


						ReadableProductOption readableOption = new ReadableProductOption(); // that is the property
/**********************************
 * CAST-Finding START #3 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `ReadableProductOption readableOption = new ReadableProductOption();` is most likely affected. - Reasoning: It instantiates a new object inside a loop, which can lead to unnecessary memory allocation and decreased performance. - Proposed solution: Move the instantiation of `ReadableProductOption` outside the loop and reuse the same object.  The code line `ReadableProductPropertyValue readableOptionValue = new ReadableProductPropertyValue();` is most likely affected. - Reasoning: It instantiates a new object inside a loop, which can lead to unnecessary memory allocation and decreased performance. - Proposed solution: Move the instantiation of `ReadableProductPropertyValue` outside the loop and reuse the same object.
 * INSTRUCTION: Please follow the OUTLINE and conduct the proposed steps with the affected code.
 * STATUS: REVIEWED
 * CAST-Finding END #3
 **********************************/
 * CAST-Finding END #3
 **********************************/
 * CAST-Finding END #3
 **********************************/


						ReadableProductPropertyValue readableOptionValue = new ReadableProductPropertyValue();

						readableOption.setCode(attribute.getProductOption().getCode());
						readableOption.setId(attribute.getProductOption().getId());

/**********************************
 * CAST-Finding START #4 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid nested loops
 * DESCRIPTION: This rule finds all loops containing nested loops.  Nested loops can be replaced by redesigning data with hashmap, or in some contexts, by using specialized high level API...  With hashmap: The literature abounds with documentation to reduce complexity of nested loops by using hashmap.  The principle is the following : having two sets of data, and two nested loops iterating over them. The complexity of a such algorithm is O(n^2). We can replace that by this process : - create an intermediate hashmap summarizing the non-null interaction between elements of both data set. This is a O(n) operation. - execute a loop over one of the data set, inside which the hash indexation to interact with the other data set is used. This is a O(n) operation.  two O(n) algorithms chained are always more efficient than a single O(n^2) algorithm.  Note : if the interaction between the two data sets is a full matrice, the optimization will not work because the O(n^2) complexity will be transferred in the hashmap creation. But it is not the main situation.  Didactic example in Perl technology: both functions do the same job. But the one using hashmap is the most efficient.  my $a = 10000; my $b = 10000;  sub withNestedLoops() {     my $i=0;     my $res;     while ($i < $a) {         print STDERR "$i\n";         my $j=0;         while ($j < $b) {             if ($i==$j) {                 $res = $i*$j;             }             $j++;         }         $i++;     } }  sub withHashmap() {     my %hash = ();          my $j=0;     while ($j < $b) {         $hash{$j} = $i*$i;         $j++;     }          my $i = 0;     while ($i < $a) {         print STDERR "$i\n";         $res = $hash{i};         $i++;     } } # takes ~6 seconds withNestedLoops();  # takes ~1 seconds withHashmap();
 * OUTLINE: The code line `if (podescriptions != null && podescriptions.size() > 0) {` is most likely affected.  - Reasoning: This line checks if the `podescriptions` set is not null and has elements, which indicates that there is a nested loop inside the if block.  - Proposed solution: To address the finding, you can optimize the loop by using the enhanced for loop instead of the traditional for loop. This can improve readability and reduce the chance of introducing bugs. For example:    ```java    for (ProductOptionDescription optionDescription : podescriptions) {        if (optionDescription.getLanguage().getCode().equals(language.getCode())) {            readableOption.setName(optionDescription.getName());        }    }    ```    can be rewritten as:    ```java    podescriptions.stream()        .filter(optionDescription -> optionDescription.getLanguage().getCode().equals(language.getCode()))        .findFirst()        .ifPresent(optionDescription -> readableOption.setName(optionDescription.getName())); 
 * INSTRUCTION: Please follow the OUTLINE and conduct the proposed steps with the affected code.
 * STATUS: REVIEWED
 * CAST-Finding END #4
 **********************************/
 * STATUS: IN_PROGRESS
 * CAST-Finding END #4
 **********************************/
 * STATUS: OPEN
 * CAST-Finding END #4
 **********************************/


							for (ProductOptionDescription optionDescription : podescriptions) {
								if (optionDescription.getLanguage().getCode().equals(language.getCode())) {
									readableOption.setName(optionDescription.getName());
								}
							}
						}

						property.setProperty(readableOption);

						Set<ProductOptionValueDescription> povdescriptions = attribute.getProductOptionValue()
/**********************************
 * CAST-Finding START #5 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid nested loops
 * DESCRIPTION: This rule finds all loops containing nested loops.  Nested loops can be replaced by redesigning data with hashmap, or in some contexts, by using specialized high level API...  With hashmap: The literature abounds with documentation to reduce complexity of nested loops by using hashmap.  The principle is the following : having two sets of data, and two nested loops iterating over them. The complexity of a such algorithm is O(n^2). We can replace that by this process : - create an intermediate hashmap summarizing the non-null interaction between elements of both data set. This is a O(n) operation. - execute a loop over one of the data set, inside which the hash indexation to interact with the other data set is used. This is a O(n) operation.  two O(n) algorithms chained are always more efficient than a single O(n^2) algorithm.  Note : if the interaction between the two data sets is a full matrice, the optimization will not work because the O(n^2) complexity will be transferred in the hashmap creation. But it is not the main situation.  Didactic example in Perl technology: both functions do the same job. But the one using hashmap is the most efficient.  my $a = 10000; my $b = 10000;  sub withNestedLoops() {     my $i=0;     my $res;     while ($i < $a) {         print STDERR "$i\n";         my $j=0;         while ($j < $b) {             if ($i==$j) {                 $res = $i*$j;             }             $j++;         }         $i++;     } }  sub withHashmap() {     my %hash = ();          my $j=0;     while ($j < $b) {         $hash{$j} = $i*$i;         $j++;     }          my $i = 0;     while ($i < $a) {         print STDERR "$i\n";         $res = $hash{i};         $i++;     } } # takes ~6 seconds withNestedLoops();  # takes ~1 seconds withHashmap();
 * OUTLINE: The code line `if (povdescriptions != null && povdescriptions.size() > 0) {` is most likely affected. - Reasoning: This line checks if `povdescriptions` is not null and has a size greater than 0, which is related to the finding of avoiding nested loops. - Proposed solution: Restructure the code to avoid nested loops by using a hashmap or specialized high-level API.  The code line `for (ProductOptionValueDescription optionValueDescription : povdescriptions) {` is most likely affected. - Reasoning: This line iterates over `povdescriptions`, which is related to the finding of avoiding nested loops. - Proposed solution: Restructure the code to avoid nested loops by using a hashmap or specialized high-level API.  The code line `if (optionValueDescription.getLanguage().getCode().equals(language.getCode())) {` is most likely affected. - Reasoning: This line checks if the language code of `optionValueDescription` is equal to the language code of `language`, which is related to the finding of avoiding nested loops. - Proposed solution: Restructure the code to avoid nested loops by using a hashmap or specialized high-level API.  The code line `readableOptionValue.setName(optionValueDescription.getName());` is most likely affected. - Reasoning: This line sets the name of `readableOptionValue` based on the name of `optionValueDescription`, which is related to the finding of avoiding nested loops. - Proposed solution: Restructure the code to avoid nested loops by using a hashmap or specialized high-level API.
 * INSTRUCTION: Please follow the OUTLINE and conduct the proposed steps with the affected code.
 * STATUS: REVIEWED
 * CAST-Finding END #5
 **********************************/
 * OUTLINE: The code line `if (povdescriptions != null && povdescriptions.size() > 0) {` is most likely affected. - Reasoning: This line checks if `povdescriptions` is not null and has a size greater than 0, which is related to the finding of avoiding nested loops. - Proposed solution: Restructure the code to avoid nested loops by using a hashmap or specialized high-level API.  The code line `for (ProductOptionValueDescription optionValueDescription : povdescriptions) {` is most likely affected. - Reasoning: This line iterates over `povdescriptions`, which is related to the finding of avoiding nested loops. - Proposed solution: Restructure the code to avoid nested loops by using a hashmap or specialized high-level API.  The code line `if (optionValueDescription.getLanguage().getCode().equals(language.getCode())) {` is most likely affected. - Reasoning: This line checks if the language code of `optionValueDescription` is equal to the language code of `language`, which is related to the finding of avoiding nested loops. - Proposed solution: Restructure the code to avoid nested loops by using a hashmap or specialized high-level API.  The code line `readableOptionValue.setName(optionValueDescription.getName());` is most likely affected. - Reasoning: This line sets the name of `readableOptionValue` based on the name of `optionValueDescription`, which is related to the finding of avoiding nested loops. - Proposed solution: Restructure the code to avoid nested loops by using a hashmap or specialized high-level API.
 * INSTRUCTION: {instruction}
 * STATUS: IN_PROGRESS
 * CAST-Finding END #5
 **********************************/
 * DESCRIPTION: This rule finds all loops containing nested loops.  Nested loops can be replaced by redesigning data with hashmap, or in some contexts, by using specialized high level API...  With hashmap: The literature abounds with documentation to reduce complexity of nested loops by using hashmap.  The principle is the following : having two sets of data, and two nested loops iterating over them. The complexity of a such algorithm is O(n^2). We can replace that by this process : - create an intermediate hashmap summarizing the non-null interaction between elements of both data set. This is a O(n) operation. - execute a loop over one of the data set, inside which the hash indexation to interact with the other data set is used. This is a O(n) operation.  two O(n) algorithms chained are always more efficient than a single O(n^2) algorithm.  Note : if the interaction between the two data sets is a full matrice, the optimization will not work because the O(n^2) complexity will be transferred in the hashmap creation. But it is not the main situation.  Didactic example in Perl technology: both functions do the same job. But the one using hashmap is the most efficient.  my $a = 10000; my $b = 10000;  sub withNestedLoops() {     my $i=0;     my $res;     while ($i < $a) {         print STDERR "$i\n";         my $j=0;         while ($j < $b) {             if ($i==$j) {                 $res = $i*$j;             }             $j++;         }         $i++;     } }  sub withHashmap() {     my %hash = ();          my $j=0;     while ($j < $b) {         $hash{$j} = $i*$i;         $j++;     }          my $i = 0;     while ($i < $a) {         print STDERR "$i\n";         $res = $hash{i};         $i++;     } } # takes ~6 seconds withNestedLoops();  # takes ~1 seconds withHashmap();
 * STATUS: OPEN
 * CAST-Finding END #5
 **********************************/


							for (ProductOptionValueDescription optionValueDescription : povdescriptions) {
								if (optionValueDescription.getLanguage().getCode().equals(language.getCode())) {
									readableOptionValue.setName(optionValueDescription.getName());
								}
							}
						}

						property.setPropertyValue(readableOptionValue);
						destination.getProperties().add(property);

					} else {// selectable option

						/**
						 * Returns a list of ReadableProductOptions
/**********************************
 * CAST-Finding START #6 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `if (selectableOptions == null) {` is most likely affected. - Reasoning: This line initializes the `selectableOptions` variable, which is mentioned in the CAST-Finding comment block. - Proposed solution: Move the instantiation of `selectableOptions` outside of the loop to avoid unnecessary object instantiations.
 * INSTRUCTION: Please follow the OUTLINE and conduct the proposed steps with the affected code.
 * STATUS: REVIEWED
 * CAST-Finding END #6
 **********************************/
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `if (selectableOptions == null) {` is most likely affected. - Reasoning: This line initializes the `selectableOptions` variable, which is mentioned in the CAST-Finding comment block. - Proposed solution: Move the instantiation of `selectableOptions` outside of the loop to avoid unnecessary object instantiations.
 * INSTRUCTION: {instruction}
 * STATUS: IN_PROGRESS
 * CAST-Finding END #6
 **********************************/
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * STATUS: OPEN
 * CAST-Finding END #6
 **********************************/


							selectableOptions = new TreeMap<Long, ReadableProductOption>();
						}
/**********************************
 * CAST-Finding START #7 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `if (opt == null) {` is most likely affected. - Reasoning: This line checks if the value retrieved from the map is null and then proceeds to create a new option if it is null. This instantiation inside the loop can be avoided as per the finding. - Proposed solution: Move the instantiation of `opt` outside the loop and change its value inside the loop instead of creating a new instance at each iteration. This will avoid unnecessary instantiations and improve performance.
 * INSTRUCTION: Please follow the OUTLINE and conduct the proposed steps with the affected code.
 * STATUS: REVIEWED
 * CAST-Finding END #7
 **********************************/
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `if (opt == null) {` is most likely affected. - Reasoning: This line checks if the value retrieved from the map is null and then proceeds to create a new option if it is null. This instantiation inside the loop can be avoided as per the finding. - Proposed solution: Move the instantiation of `opt` outside the loop and change its value inside the loop instead of creating a new instance at each iteration. This will avoid unnecessary instantiations and improve performance.
 * INSTRUCTION: {instruction}
 * STATUS: IN_PROGRESS
 * CAST-Finding END #7
 **********************************/
 * CAST-Finding START #7 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * STATUS: OPEN
 * CAST-Finding END #7
/**********************************
 * CAST-Finding START #8 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `optValue.setId(attribute.getId());` is most likely affected. - Reasoning: The ID of `optValue` is set based on `attribute.getId()`. If `attribute.getId()` is changed inside the loop, it may result in unnecessary instantiations. - Proposed solution: Move the instantiation of `optValue` outside the loop and update its ID inside the loop instead of creating a new instance at each iteration.
 * INSTRUCTION: Please follow the OUTLINE and conduct the proposed steps with the affected code.
 * STATUS: REVIEWED
 * CAST-Finding END #8
 **********************************/
 * CAST-Finding START #8 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `optValue.setId(attribute.getId());` is most likely affected. - Reasoning: The ID of `optValue` is set based on `attribute.getId()`. If `attribute.getId()` is changed inside the loop, it may result in unnecessary instantiations. - Proposed solution: Move the instantiation of `optValue` outside the loop and update its ID inside the loop instead of creating a new instance at each iteration.
 * INSTRUCTION: {instruction}
 * STATUS: IN_PROGRESS
 * CAST-Finding END #8
 **********************************/
/**********************************
 * CAST-Finding START #8 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * STATUS: OPEN
 * CAST-Finding END #8
 **********************************/

/**********************************
 * CAST-Finding START #9 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `if (attribute.getProductAttributePrice() != null && attribute.getProductAttributePrice().doubleValue() > 0) {` is most likely affected.  - Reasoning: It is inside the code block where the CAST-Finding is located.  - Proposed solution: Move the instantiation of `String formatedPrice = null;` outside the if statement to avoid instantiating it at each iteration. Move the method call `pricingService.getDisplayAmount(attribute.getProductAttributePrice(), store);` outside the if statement and store the result in a variable before setting it to `optValue.setPrice(formatedPrice);`.
 * INSTRUCTION: Please follow the OUTLINE and conduct the proposed steps with the affected code.
 * STATUS: REVIEWED
 * CAST-Finding END #9
 **********************************/
/**********************************
 * CAST-Finding START #9 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `if (attribute.getProductAttributePrice() != null && attribute.getProductAttributePrice().doubleValue() > 0) {` is most likely affected.  - Reasoning: It is inside the code block where the CAST-Finding is located.  - Proposed solution: Move the instantiation of `String formatedPrice = null;` outside the if statement to avoid instantiating it at each iteration. Move the method call `pricingService.getDisplayAmount(attribute.getProductAttributePrice(), store);` outside the if statement and store the result in a variable before setting it to `optValue.setPrice(formatedPrice);`.
 * INSTRUCTION: {instruction}
 * STATUS: IN_PROGRESS
 * CAST-Finding END #9
 **********************************/

/**********************************
 * CAST-Finding START #9 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * STATUS: OPEN
 * CAST-Finding END #9
 **********************************/


								throw new ConversionRuntimeException(
										"Error converting product option, an exception occured with pricingService", e);
							}
						}

/**********************************
 * CAST-Finding START #10 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid nested loops
 * DESCRIPTION: This rule finds all loops containing nested loops.  Nested loops can be replaced by redesigning data with hashmap, or in some contexts, by using specialized high level API...  With hashmap: The literature abounds with documentation to reduce complexity of nested loops by using hashmap.  The principle is the following : having two sets of data, and two nested loops iterating over them. The complexity of a such algorithm is O(n^2). We can replace that by this process : - create an intermediate hashmap summarizing the non-null interaction between elements of both data set. This is a O(n) operation. - execute a loop over one of the data set, inside which the hash indexation to interact with the other data set is used. This is a O(n) operation.  two O(n) algorithms chained are always more efficient than a single O(n^2) algorithm.  Note : if the interaction between the two data sets is a full matrice, the optimization will not work because the O(n^2) complexity will be transferred in the hashmap creation. But it is not the main situation.  Didactic example in Perl technology: both functions do the same job. But the one using hashmap is the most efficient.  my $a = 10000; my $b = 10000;  sub withNestedLoops() {     my $i=0;     my $res;     while ($i < $a) {         print STDERR "$i\n";         my $j=0;         while ($j < $b) {             if ($i==$j) {                 $res = $i*$j;             }             $j++;         }         $i++;     } }  sub withHashmap() {     my %hash = ();          my $j=0;     while ($j < $b) {         $hash{$j} = $i*$i;         $j++;     }          my $i = 0;     while ($i < $a) {         print STDERR "$i\n";         $res = $hash{i};         $i++;     } } # takes ~6 seconds withNestedLoops();  # takes ~1 seconds withHashmap();
 * OUTLINE: The code line `optValue.setSortOrder(attribute.getProductOptionSortOrder().intValue());` is most likely affected.  - Reasoning: This line sets the sort order of `optValue` based on the `ProductOptionSortOrder` attribute of `attribute`.  - Proposed solution: No solution proposed.  NOT APPLICABLE. No code obviously affected.
 * INSTRUCTION: Please follow the OUTLINE and conduct the proposed steps with the affected code.
 * STATUS: REVIEWED
 * CAST-Finding END #10
 **********************************/

/**********************************
 * CAST-Finding START #10 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid nested loops
 * DESCRIPTION: This rule finds all loops containing nested loops.  Nested loops can be replaced by redesigning data with hashmap, or in some contexts, by using specialized high level API...  With hashmap: The literature abounds with documentation to reduce complexity of nested loops by using hashmap.  The principle is the following : having two sets of data, and two nested loops iterating over them. The complexity of a such algorithm is O(n^2). We can replace that by this process : - create an intermediate hashmap summarizing the non-null interaction between elements of both data set. This is a O(n) operation. - execute a loop over one of the data set, inside which the hash indexation to interact with the other data set is used. This is a O(n) operation.  two O(n) algorithms chained are always more efficient than a single O(n^2) algorithm.  Note : if the interaction between the two data sets is a full matrice, the optimization will not work because the O(n^2) complexity will be transferred in the hashmap creation. But it is not the main situation.  Didactic example in Perl technology: both functions do the same job. But the one using hashmap is the most efficient.  my $a = 10000; my $b = 10000;  sub withNestedLoops() {     my $i=0;     my $res;     while ($i < $a) {         print STDERR "$i\n";         my $j=0;         while ($j < $b) {             if ($i==$j) {                 $res = $i*$j;             }             $j++;         }         $i++;     } }  sub withHashmap() {     my %hash = ();          my $j=0;     while ($j < $b) {         $hash{$j} = $i*$i;         $j++;     }          my $i = 0;     while ($i < $a) {         print STDERR "$i\n";         $res = $hash{i};         $i++;     } } # takes ~6 seconds withNestedLoops();  # takes ~1 seconds withHashmap();
 * OUTLINE: The code line `optValue.setSortOrder(attribute.getProductOptionSortOrder().intValue());` is most likely affected.  - Reasoning: This line sets the sort order of `optValue` based on the `ProductOptionSortOrder` attribute of `attribute`.  - Proposed solution: No solution proposed.  NOT APPLICABLE. No code obviously affected.
 * INSTRUCTION: {instruction}
 * STATUS: IN_PROGRESS
 * CAST-Finding END #10
 **********************************/


/**********************************
 * CAST-Finding START #10 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid nested loops
 * DESCRIPTION: This rule finds all loops containing nested loops.  Nested loops can be replaced by redesigning data with hashmap, or in some contexts, by using specialized high level API...  With hashmap: The literature abounds with documentation to reduce complexity of nested loops by using hashmap.  The principle is the following : having two sets of data, and two nested loops iterating over them. The complexity of a such algorithm is O(n^2). We can replace that by this process : - create an intermediate hashmap summarizing the non-null interaction between elements of both data set. This is a O(n) operation. - execute a loop over one of the data set, inside which the hash indexation to interact with the other data set is used. This is a O(n) operation.  two O(n) algorithms chained are always more efficient than a single O(n^2) algorithm.  Note : if the interaction between the two data sets is a full matrice, the optimization will not work because the O(n^2) complexity will be transferred in the hashmap creation. But it is not the main situation.  Didactic example in Perl technology: both functions do the same job. But the one using hashmap is the most efficient.  my $a = 10000; my $b = 10000;  sub withNestedLoops() {     my $i=0;     my $res;     while ($i < $a) {         print STDERR "$i\n";         my $j=0;         while ($j < $b) {             if ($i==$j) {                 $res = $i*$j;             }             $j++;         }         $i++;     } }  sub withHashmap() {     my %hash = ();          my $j=0;     while ($j < $b) {         $hash{$j} = $i*$i;         $j++;     }          my $i = 0;     while ($i < $a) {         print STDERR "$i\n";         $res = $hash{i};         $i++;     } } # takes ~6 seconds withNestedLoops();  # takes ~1 seconds withHashmap();
 * STATUS: OPEN
 * CAST-Finding END #10
 **********************************/


								for (ProductOptionValueDescription optionValueDescription : podescriptions) {
									if (optionValueDescription.getLanguage().getId().intValue() == language.getId()
											.intValue()) {
										podescription = optionValueDescription;
										break;
									}
								}
							}
						}
						valueDescription.setName(podescription.getName());
						valueDescription.setDescription(podescription.getDescription());
						optValue.setDescription(valueDescription);

						if (opt != null) {
							opt.getOptionValues().add(optValue);
						}
					}
				}
			}
		}
		
		ReadableProductVariant defaultInstance = null;

		// variants
		if (!CollectionUtils.isEmpty(source.getVariants()))

		{
			List<ReadableProductVariant> instances = source
					.getVariants().stream()
					.map(i -> readableProductVariantMapper.convert(i, store, language)).collect(Collectors.toList());
			destination.setVariants(instances);
			
			/**
			 * When an item has instances
			 * Take default instance
			 * 
			 * - Set item price as default instance price
			 * - Set default image as default instance image
			 */
			
			//get default instance
			defaultInstance = instances.stream().filter(i -> i.isDefaultSelection()).findAny().orElse(null);
			

			/**
			 * variants options list variation color
			 */

			/**
			 * Returns a list of ReadableProductOptions
			 * 
			 * name lang type code List ReadableProductOptionValueEntity name description
			 * image order default
			 */

			/**
			 * Create options from instance Create a list of option values
			 */

			for (ProductVariant instance : source.getVariants()) {
				instanceToOption(selectableOptions, instance, store, language);
			}

		}

		if (selectableOptions != null) {
			List<ReadableProductOption> options = new ArrayList<ReadableProductOption>(selectableOptions.values());
			destination.setOptions(options);
		}
		
		// availability
		ProductAvailability availability = null;
		for (ProductAvailability a : source.getAvailabilities()) {
			// TODO validate region
			// if(availability.getRegion().equals(Constants.ALL_REGIONS)) {//TODO REL 3.X
			// accept a region
			
			/**
			 * Default availability
			 * store
			 * product
			 * instance null
			 * region variant null
			 */
			
			
			availability = a;
			destination.setQuantity(availability.getProductQuantity() == null ? 1 : availability.getProductQuantity());
			destination.setQuantityOrderMaximum(
					availability.getProductQuantityOrderMax() == null ? 1 : availability.getProductQuantityOrderMax());
			destination.setQuantityOrderMinimum(
					availability.getProductQuantityOrderMin() == null ? 1 : availability.getProductQuantityOrderMin());
			if (availability.getProductQuantity().intValue() > 0 && destination.isAvailable()) {
				destination.setCanBePurchased(true);
			}
			
			if(a.getProductVariant()==null && StringUtils.isEmpty(a.getRegionVariant())) {
				break;
			}
		}
		
		//if default instance

		destination.setSku(source.getSku());

		try {
			FinalPrice price = pricingService.calculateProductPrice(source);
			if (price != null) {

				destination.setFinalPrice(pricingService.getDisplayAmount(price.getFinalPrice(), store));
				destination.setPrice(price.getFinalPrice());
				destination.setOriginalPrice(pricingService.getDisplayAmount(price.getOriginalPrice(), store));

				if (price.isDiscounted()) {
					destination.setDiscounted(true);
				}

				// price appender
				if (availability != null) {
					Set<ProductPrice> prices = availability.getPrices();
					if (!CollectionUtils.isEmpty(prices)) {
						ReadableProductPrice readableProductPrice = new ReadableProductPrice();
						readableProductPrice.setDiscounted(destination.isDiscounted());
						readableProductPrice.setFinalPrice(destination.getFinalPrice());
						readableProductPrice.setOriginalPrice(destination.getOriginalPrice());

						Optional<ProductPrice> pr = prices.stream()
								.filter(p -> p.getCode().equals(ProductPrice.DEFAULT_PRICE_CODE)).findFirst();

						destination.setProductPrice(readableProductPrice);

						if (pr.isPresent() && language !=null) {
							readableProductPrice.setId(pr.get().getId());
							Optional<ProductPriceDescription> d = pr.get().getDescriptions().stream()
									.filter(desc -> desc.getLanguage().getCode().equals(language.getCode()))
									.findFirst();
							if (d.isPresent()) {
								com.salesmanager.shop.model.catalog.product.ProductPriceDescription priceDescription = new com.salesmanager.shop.model.catalog.product.ProductPriceDescription();
								priceDescription.setLanguage(language.getCode());
								priceDescription.setId(d.get().getId());
								priceDescription.setPriceAppender(d.get().getPriceAppender());
								readableProductPrice.setDescription(priceDescription);
							}
						}

					}
				}

			}

		} catch (Exception e) {
			throw new ConversionRuntimeException("An error while converting product price", e);
		}

		if (source.getProductReviewAvg() != null) {
			double avg = source.getProductReviewAvg().doubleValue();
			double rating = Math.round(avg * 2) / 2.0f;
			destination.setRating(rating);
		}

		if (source.getProductReviewCount() != null) {
			destination.setRatingCount(source.getProductReviewCount().intValue());
		}

		if (description != null) {
			com.salesmanager.shop.model.catalog.product.ProductDescription tragetDescription = populateDescription(
					description);
			destination.setDescription(tragetDescription);

		}

		if (!CollectionUtils.isEmpty(source.getCategories())) {
			List<ReadableCategory> categoryList = new ArrayList<ReadableCategory>();
			for (Category category : source.getCategories()) {
				ReadableCategory readableCategory = readableCategoryMapper.convert(category, store, language);
				categoryList.add(readableCategory);

			}
			destination.setCategories(categoryList);
		}

		ProductSpecification specifications = new ProductSpecification();
		specifications.setHeight(source.getProductHeight());
		specifications.setLength(source.getProductLength());
		specifications.setWeight(source.getProductWeight());
		specifications.setWidth(source.getProductWidth());
		if (!StringUtils.isBlank(store.getSeizeunitcode())) {
			specifications
					.setDimensionUnitOfMeasure(DimensionUnitOfMeasure.valueOf(store.getSeizeunitcode().toLowerCase()));
		}
		if (!StringUtils.isBlank(store.getWeightunitcode())) {
			specifications.setWeightUnitOfMeasure(WeightUnitOfMeasure.valueOf(store.getWeightunitcode().toLowerCase()));
		}
		destination.setProductSpecifications(specifications);

		destination.setSortOrder(source.getSortOrder());

		return destination;
	}

	private ReadableImage convertImage(Product product, ProductImage image, MerchantStore store) {
		ReadableImage prdImage = new ReadableImage();
		prdImage.setImageName(image.getProductImage());
		prdImage.setDefaultImage(image.isDefaultImage());

		// TODO product variant image
		StringBuilder imgPath = new StringBuilder();
		imgPath.append(imageUtils.getContextPath())
				.append(imageUtils.buildProductImageUtils(store, product.getSku(), image.getProductImage()));

		prdImage.setImageUrl(imgPath.toString());
		prdImage.setId(image.getId());
		prdImage.setImageType(image.getImageType());
		if (image.getProductImageUrl() != null) {
			prdImage.setExternalUrl(image.getProductImageUrl());
		}
		if (image.getImageType() == 1 && image.getProductImageUrl() != null) {// video
			prdImage.setVideoUrl(image.getProductImageUrl());
		}

		if (prdImage.isDefaultImage()) {
			prdImage.setDefaultImage(true);
		}

		return prdImage;
	}

	private com.salesmanager.shop.model.catalog.product.ProductDescription populateDescription(
			ProductDescription description) {
		if (description == null) {
			return null;
		}

		com.salesmanager.shop.model.catalog.product.ProductDescription tragetDescription = new com.salesmanager.shop.model.catalog.product.ProductDescription();
		tragetDescription.setFriendlyUrl(description.getSeUrl());
		tragetDescription.setName(description.getName());
		tragetDescription.setId(description.getId());
		if (!StringUtils.isBlank(description.getMetatagTitle())) {
			tragetDescription.setTitle(description.getMetatagTitle());
		} else {
			tragetDescription.setTitle(description.getName());
		}
		tragetDescription.setMetaDescription(description.getMetatagDescription());
		tragetDescription.setDescription(description.getDescription());
		tragetDescription.setHighlights(description.getProductHighlight());
		tragetDescription.setLanguage(description.getLanguage().getCode());
		tragetDescription.setKeyWords(description.getMetatagKeywords());

		if (description.getLanguage() != null) {
			tragetDescription.setLanguage(description.getLanguage().getCode());
/**********************************
 * CAST-Finding START #11 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `List<ProductOptionDescription> descriptions = productAttribute.getProductOption().getDescriptionsSettoList();` is most likely affected. - Reasoning: The line instantiates a new `List` object inside the loop, which is a memory-intensive operation and can impact performance. - Proposed solution: Move the instantiation of the `List` object outside the loop to avoid unnecessary object creation.
 * INSTRUCTION: Please follow the OUTLINE and conduct the proposed steps with the affected code.
 * STATUS: REVIEWED
 * CAST-Finding END #11
 **********************************/
		attr.setType(productAttribute.getProductOption().getProductOptionType());

/**********************************
 * CAST-Finding START #11 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `List<ProductOptionDescription> descriptions = productAttribute.getProductOption().getDescriptionsSettoList();` is most likely affected. - Reasoning: The line instantiates a new `List` object inside the loop, which is a memory-intensive operation and can impact performance. - Proposed solution: Move the instantiation of the `List` object outside the loop to avoid unnecessary object creation.
 * INSTRUCTION: {instruction}
 * STATUS: IN_PROGRESS
 * CAST-Finding END #11
 **********************************/



/**********************************
 * CAST-Finding START #11 (2024-02-01 22:20:08.211471):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * STATUS: OPEN
 * CAST-Finding END #11
 **********************************/


				com.salesmanager.shop.model.catalog.product.attribute.ProductOptionValueDescription productOptionValueDescription = new com.salesmanager.shop.model.catalog.product.attribute.ProductOptionValueDescription();
				productOptionValueDescription.setId(optionDescription.getId());
				productOptionValueDescription.setLanguage(optionDescription.getLanguage().getCode());
				productOptionValueDescription.setName(optionDescription.getName());
				propertyValue.getValues().add(productOptionValueDescription);

			}
		}

		attr.setCode(productAttribute.getProductOption().getCode());
		return attr;

	}

	private Optional<ReadableProductOptionValue> optionValue(ProductOptionValue optionValue, MerchantStore store,
			Language language) {

		if (optionValue == null) {
			return Optional.empty();
		}

		ReadableProductOptionValue optValue = new ReadableProductOptionValue();

		com.salesmanager.shop.model.catalog.product.attribute.ProductOptionValueDescription valueDescription = new com.salesmanager.shop.model.catalog.product.attribute.ProductOptionValueDescription();
		valueDescription.setLanguage(language.getCode());

		if (!StringUtils.isBlank(optionValue.getProductOptionValueImage())) {
			optValue.setImage(
					imageUtils.buildProductPropertyImageUtils(store, optionValue.getProductOptionValueImage()));
		}
		optValue.setSortOrder(0);

		if (optionValue.getProductOptionValueSortOrder() != null) {
			optValue.setSortOrder(optionValue.getProductOptionValueSortOrder().intValue());
		}

		optValue.setCode(optionValue.getCode());

		List<ProductOptionValueDescription> podescriptions = optionValue.getDescriptionsSettoList();
		ProductOptionValueDescription podescription = null;
		if (podescriptions != null && podescriptions.size() > 0) {
			podescription = podescriptions.get(0);
			if (podescriptions.size() > 1) {
				for (ProductOptionValueDescription optionValueDescription : podescriptions) {
					if (optionValueDescription.getLanguage().getId().intValue() == language.getId().intValue()) {
						podescription = optionValueDescription;
						break;
					}
				}
			}
		}
		valueDescription.setName(podescription.getName());
		valueDescription.setDescription(podescription.getDescription());
		optValue.setDescription(valueDescription);

		return Optional.of(optValue);

	}

	private void instanceToOption(TreeMap<Long, ReadableProductOption> selectableOptions, ProductVariant instance,
			MerchantStore store, Language language) {


		ReadableProductOption option = this.option(selectableOptions, instance.getVariation().getProductOption(), language);
		option.setVariant(true);


		// take care of option value
		Optional<ReadableProductOptionValue> optionOptionValue = this
				.optionValue(instance.getVariation().getProductOptionValue(), store, language);

		if (optionOptionValue.isPresent()) {
			optionOptionValue.get().setId(instance.getId());
			if (instance.isDefaultSelection()) {
				optionOptionValue.get().setDefaultValue(true);
			}
			addOptionValue(option, optionOptionValue.get());

		}

		if (instance.getVariationValue() != null) {
			ReadableProductOption optionValue = this.option(selectableOptions, instance.getVariationValue().getProductOption(), language);

			// take care of option value
			Optional<ReadableProductOptionValue> optionValueOptionValue = this
					.optionValue(instance.getVariationValue().getProductOptionValue(), store, language);


			if (optionValueOptionValue.isPresent()) {
				optionValueOptionValue.get().setId(instance.getId());
				if (instance.isDefaultSelection()) {
					optionValueOptionValue.get().setDefaultValue(true);
				}
				addOptionValue(optionValue, optionValueOptionValue.get());
			}

		}

	}
	
	private void addOptionValue(ReadableProductOption option, ReadableProductOptionValue optionValue) {
		
		ReadableProductOptionValue find = option.getOptionValues().stream()
				  .filter(optValue -> optValue.getCode()==optionValue.getCode())
				  .findAny()
				  .orElse(null);
		
		if(find == null) {
			option.getOptionValues().add(optionValue);
		}
	}
	
	private ReadableProductOption option(TreeMap<Long, ReadableProductOption> selectableOptions, ProductOption option, Language language) {
		if(selectableOptions.containsKey(option.getId())) {
			return selectableOptions.get(option.getId());
		}

		ReadableProductOption readable = this.createOption(option, language);
		selectableOptions.put(readable.getId(), readable);
		return readable;
	}

	private ReadableProductOption createOption(ProductOption opt, Language language) {

		ReadableProductOption option = new ReadableProductOption();
		option.setId(opt.getId());// attribute of the option
		option.setType(opt.getProductOptionType());
		option.setCode(opt.getCode());
		List<ProductOptionDescription> descriptions = opt.getDescriptionsSettoList();
		ProductOptionDescription description = null;
		if (descriptions != null && descriptions.size() > 0) {
			description = descriptions.get(0);
			if (descriptions.size() > 1) {
				for (ProductOptionDescription optionDescription : descriptions) {
					if (optionDescription.getLanguage().getCode().equals(language.getCode())) {
						description = optionDescription;
						break;
					}
				}
			}
		}

		if (description == null) {
			return null;
		}

		option.setLang(language.getCode());
		option.setName(description.getName());
		option.setCode(opt.getCode());

		return option;

	}

}
