package com.salesmanager.shop.populator.order;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import com.salesmanager.core.business.exception.ConversionException;
import com.salesmanager.core.business.services.catalog.product.ProductService;
import com.salesmanager.core.business.services.catalog.product.attribute.ProductAttributeService;
import com.salesmanager.core.business.services.catalog.product.file.DigitalProductService;
import com.salesmanager.core.business.utils.AbstractDataPopulator;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.file.DigitalProduct;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.orderproduct.OrderProduct;
import com.salesmanager.core.model.order.orderproduct.OrderProductAttribute;
import com.salesmanager.core.model.order.orderproduct.OrderProductDownload;
import com.salesmanager.core.model.order.orderproduct.OrderProductPrice;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.constants.ApplicationConstants;
import com.salesmanager.shop.model.catalog.product.attribute.ProductAttribute;
import com.salesmanager.shop.model.order.PersistableOrderProduct;

public class PersistableOrderProductPopulator extends
		AbstractDataPopulator<PersistableOrderProduct, OrderProduct> {
	
	private ProductService productService;
	private DigitalProductService digitalProductService;
	private ProductAttributeService productAttributeService;


	public ProductAttributeService getProductAttributeService() {
		return productAttributeService;
	}

	public void setProductAttributeService(
			ProductAttributeService productAttributeService) {
		this.productAttributeService = productAttributeService;
	}

	public DigitalProductService getDigitalProductService() {
		return digitalProductService;
	}

	public void setDigitalProductService(DigitalProductService digitalProductService) {
		this.digitalProductService = digitalProductService;
	}

	/**
	 * Converts a ShoppingCartItem carried in the ShoppingCart to an OrderProduct
	 * that will be saved in the system
	 */
	@Override
	public OrderProduct populate(PersistableOrderProduct source, OrderProduct target,
			MerchantStore store, Language language) throws ConversionException {
		
		Validate.notNull(productService,"productService must be set");
		Validate.notNull(digitalProductService,"digitalProductService must be set");
		Validate.notNull(productAttributeService,"productAttributeService must be set");

		
		try {
			Product modelProduct = productService.getById(source.getProduct().getId());
			if(modelProduct==null) {
				throw new ConversionException("Cannot get product with id (productId) " + source.getProduct().getId());
			}
			
			if(modelProduct.getMerchantStore().getId().intValue()!=store.getId().intValue()) {
				throw new ConversionException("Invalid product id " + source.getProduct().getId());
			}

			DigitalProduct digitalProduct = digitalProductService.getByProduct(store, modelProduct);
			
			if(digitalProduct!=null) {
				OrderProductDownload orderProductDownload = new OrderProductDownload();	
				orderProductDownload.setOrderProductFilename(digitalProduct.getProductFileName());
				orderProductDownload.setOrderProduct(target);
				orderProductDownload.setDownloadCount(0);
				orderProductDownload.setMaxdays(ApplicationConstants.MAX_DOWNLOAD_DAYS);
				target.getDownloads().add(orderProductDownload);
			}

			target.setOneTimeCharge(source.getPrice());	
			target.setProductName(source.getProduct().getDescription().getName());
			target.setProductQuantity(source.getOrderedQuantity());
			target.setSku(source.getProduct().getSku());
			
			OrderProductPrice orderProductPrice = new OrderProductPrice();
			orderProductPrice.setDefaultPrice(true);
			orderProductPrice.setProductPrice(source.getPrice());
			orderProductPrice.setOrderProduct(target);
			

			
			Set<OrderProductPrice> prices = new HashSet<OrderProductPrice>();
			prices.add(orderProductPrice);

			/** DO NOT SUPPORT MUTIPLE PRICES **/
/*			//Other prices
			List<FinalPrice> otherPrices = finalPrice.getAdditionalPrices();
			if(otherPrices!=null) {
				for(FinalPrice otherPrice : otherPrices) {
					OrderProductPrice other = orderProductPrice(otherPrice);
					other.setOrderProduct(target);
					prices.add(other);
				}
			}*/
			
			target.setPrices(prices);
			
			//OrderProductAttribute
			List<ProductAttribute> attributeItems = source.getAttributes();
			if(!CollectionUtils.isEmpty(attributeItems)) {
				Set<OrderProductAttribute> attributes = new HashSet<OrderProductAttribute>();
				for(ProductAttribute attribute : attributeItems) {




/**********************************
 * CAST-Finding START #1 (2024-02-01 22:52:18.616666):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `target.setPrices(prices);` is most likely affected. - Reasoning: It is the first line after the 'CAST-Finding' comment block and it is a method call that could potentially be optimized. - Proposed solution: To address the finding, the instantiation of the OrderProductAttribute object inside the loop can be moved outside the loop to avoid unnecessary object creation at each iteration.
 * INSTRUCTION: {instruction}
 * STATUS: IN_PROGRESS
 * CAST-Finding END #1
 **********************************/


					OrderProductAttribute orderProductAttribute = new OrderProductAttribute();
					orderProductAttribute.setOrderProduct(target);
					Long id = attribute.getId();
					com.salesmanager.core.model.catalog.product.attribute.ProductAttribute attr = productAttributeService.getById(id);
					if(attr==null) {



/**********************************
 * CAST-Finding START #2 (2024-02-01 22:52:18.616666):
 * TITLE: Avoid string concatenation in loops
 * DESCRIPTION: Avoid string concatenation inside loops.  Since strings are immutable, concatenation is a greedy operation. This creates unnecessary temporary objects and results in quadratic rather than linear running time. In a loop, instead using concatenation, add each substring to a list and join the list after the loop terminates (or, write each substring to a byte buffer).
 * OUTLINE: The code line `OrderProductAttribute orderProductAttribute = new OrderProductAttribute();` is most likely affected. - Reasoning: It instantiates a new object inside a loop, which can lead to unnecessary memory allocation and decreased performance. - Proposed solution: Move the instantiation of `OrderProductAttribute` outside of the loop if possible, to avoid unnecessary memory allocation.
 * INSTRUCTION: {instruction}
 * STATUS: IN_PROGRESS
 * CAST-Finding END #2
 **********************************/
 **********************************/




/**********************************
 * CAST-Finding START #3 (2024-02-01 22:52:18.616666):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `throw new ConversionException("Attribute id " + id + " does not exists");` is most likely affected. - Reasoning: It involves string concatenation inside a loop, which is discouraged by the finding. - Proposed solution: Instead of concatenating the string inside the loop, you can create a list to store each substring and then join the list after the loop terminates.  The code line `if(attr.getProduct().getMerchantStore().getId().intValue()!=store.getId().intValue()) {` is most likely affected. - Reasoning: It involves object instantiation inside a loop, which is discouraged by the finding. - Proposed solution: Instead of instantiating the object inside the loop, you can create the object once outside the loop and change its value at each iteration.
 * INSTRUCTION: {instruction}
 * STATUS: IN_PROGRESS
 * CAST-Finding END #3
 **********************************/
 * CAST-Finding END #3
 **********************************/


						throw new ConversionException("Attribute id " + id + " does not exists");
					}
					
					if(attr.getProduct().getMerchantStore().getId().intValue()!=store.getId().intValue()) {

/**********************************
 * CAST-Finding START #4 (2024-02-01 22:52:18.616666):
 * TITLE: Avoid string concatenation in loops
 * DESCRIPTION: Avoid string concatenation inside loops.  Since strings are immutable, concatenation is a greedy operation. This creates unnecessary temporary objects and results in quadratic rather than linear running time. In a loop, instead using concatenation, add each substring to a list and join the list after the loop terminates (or, write each substring to a byte buffer).
 * OUTLINE: The code line `throw new ConversionException("Attribute id " + id + " does not exists");` is most likely affected. - Reasoning: It involves string concatenation inside a loop, which can result in quadratic running time and unnecessary temporary objects. - Proposed solution: Avoid string concatenation inside loops. Instead, each substring should be added to a list and joined after the loop terminates.
 * INSTRUCTION: {instruction}
 * STATUS: IN_PROGRESS
 * CAST-Finding END #4
 **********************************/
 * STATUS: OPEN
 * CAST-Finding END #4
 **********************************/


/**********************************
 * CAST-Finding START #5 (2024-02-01 22:52:18.616666):
 * TITLE: Avoid instantiations inside loops
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * OUTLINE: The code line `throw new ConversionException("Attribute id " + id + " invalid for this store");` is most likely affected.  - Reasoning: It involves string concatenation inside a loop, which is discouraged by the finding.  - Proposed solution: Replace it with a solution that avoids string concatenation inside a loop, such as adding each substring to a list and joining the list after the loop terminates or writing each substring to a byte buffer.
 * INSTRUCTION: {instruction}
 * STATUS: IN_PROGRESS
 * CAST-Finding END #5
 **********************************/
 * DESCRIPTION: Object instantiation uses memory allocation, that is a greedy operation. Doing an instantiation at each iteration could really hamper the performances and increase resource usage.  If the instantiated object is local to the loop, there is absolutely no need to instantiate it at each iteration : create it once outside the loop, and just change its value at each iteration. If the object is immutable, create if possible a mutable class. If the aim is to create a consolidated data structure, then, unless the need is to release the data case by case, it could be better to make a single global allocation outside the loop, and fill it with data inside the loop.
 * STATUS: OPEN
 * CAST-Finding END #5
 **********************************/


						throw new ConversionException("Attribute id " + id + " invalid for this store");
					}
					
					orderProductAttribute.setProductAttributeIsFree(attr.getProductAttributeIsFree());
					orderProductAttribute.setProductAttributeName(attr.getProductOption().getDescriptionsSettoList().get(0).getName());
					orderProductAttribute.setProductAttributeValueName(attr.getProductOptionValue().getDescriptionsSettoList().get(0).getName());
					orderProductAttribute.setProductAttributePrice(attr.getProductAttributePrice());
					orderProductAttribute.setProductAttributeWeight(attr.getProductAttributeWeight());
					orderProductAttribute.setProductOptionId(attr.getProductOption().getId());
					orderProductAttribute.setProductOptionValueId(attr.getProductOptionValue().getId());
					attributes.add(orderProductAttribute);
				}
				target.setOrderAttributes(attributes);
			}

			
		} catch (Exception e) {
			throw new ConversionException(e);
		}
		
		
		return target;
	}

	@Override
	protected OrderProduct createTarget() {
		return null;
	}

	public void setProductService(ProductService productService) {
		this.productService = productService;
	}

	public ProductService getProductService() {
		return productService;
	}
	


}
