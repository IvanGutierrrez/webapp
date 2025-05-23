package com.webapp08.pujahoy.service;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.Blob;
import java.sql.Date;
import com.webapp08.pujahoy.repository.UserModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Pageable;

import com.webapp08.pujahoy.dto.OfferDTO;
import com.webapp08.pujahoy.dto.OfferMapper;
import com.webapp08.pujahoy.dto.ProductBasicDTO;
import com.webapp08.pujahoy.dto.ProductBasicMapper;
import com.webapp08.pujahoy.dto.ProductDTO;
import com.webapp08.pujahoy.dto.ProductMapper;
import com.webapp08.pujahoy.dto.PublicUserDTO;
import com.webapp08.pujahoy.dto.UserMapper;
import com.webapp08.pujahoy.model.Offer;
import com.webapp08.pujahoy.model.Product;
import com.webapp08.pujahoy.model.Transaction;
import com.webapp08.pujahoy.model.UserModel;
import com.webapp08.pujahoy.repository.OfferRepository;
import com.webapp08.pujahoy.repository.ProductRepository;
import com.webapp08.pujahoy.repository.TransactionRepository;

import org.springframework.core.io.Resource;

@Service
public class ProductService {

    private final UserModelRepository userModelRepository;

    @Autowired
	private ProductRepository repository;
	@Autowired
	private ProductMapper mapper;
	@Autowired
	private OfferMapper offerMapper;
	@Autowired
	private UserMapper userMapper;
	@Autowired
	private ProductBasicMapper basicMapper;
	@Autowired
    private TransactionRepository transactionRepository;
	@Autowired
    private OfferRepository offerRepository;

    ProductService(UserModelRepository userModelRepository) {
        this.userModelRepository = userModelRepository;
    }

	public Optional<ProductDTO> findById(long id) {
		Optional<Product> prod = repository.findById(id);
		if (prod.isPresent()){
			return Optional.of(mapper.toDTO(prod.get()));
		} else {
			return Optional.empty();
		}
	}

	public Product save(Product product) {
		return repository.save(product);
	}

	public Product save(ProductDTO product) {
		return repository.save(mapper.toDomain(product));
	}

	public void deleteById(long id_product) {
		Optional<Product> existingProduct = repository.findById(id_product);
		Optional<Transaction> trans = transactionRepository.findByProduct(existingProduct.get());
        if (trans.isPresent()){
            transactionRepository.deleteById(trans.get().getId());  
        }
		for (Offer offer : existingProduct.get().getOffers()) {
			offerRepository.delete(offer);
		}
        repository.deleteById(id_product);
    }

	public List<Product> findBySeller(UserModel user) {
		return repository.findBySeller(user);
	}

	//Search all
    public Page<ProductBasicDTO> obtainAllProductOrdersByReputation(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Product> productPage = repository.findAllOrderedBySellerReputation(pageable);
	
		return productPage.map(basicMapper::toDTO); 
	}
	//Search only In progress
	public Page<ProductBasicDTO> obtainAllProductOrdersInProgressByReputation(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Product> productPage = repository.findByStateInProgressOrderedBySellerReputation(pageable);
	
		return productPage.map(basicMapper::toDTO); 
	}
	//Search all
    public Page<ProductBasicDTO> obtainAllProductOrdersByReputationDTO(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
        return repository.findAllOrderedBySellerReputation(pageable).map(this::toDTO);
    }
	//Search only In progress
	public Page<ProductBasicDTO> obtainAllProductOrdersInProgressByReputationDTO(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
        return repository.findByStateInProgressOrderedBySellerReputation(pageable).map(this::toDTO);
    }

	public Page<Product> obtainAllProducts(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
        return repository.findAll(pageable);
    }

	public Page<Product> obtainPaginatedProducts(String sellerName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findBySeller_Name(sellerName, pageable);
    }

	public Page<Product> obtainProductsBuyed(String buyerName, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
        return repository.findBoughtProductsByUser(buyerName, pageable);
    }

	public ProductDTO findProduct(Long id) {
		return mapper.toDTO(repository.findById(id).get());
	}

	public Resource getPostImage(long id) throws SQLException {
		Product product = repository.findById(id).orElseThrow(() -> new NoSuchElementException("Post not found"));

		if (product.getImage() != null) {
			return new InputStreamResource(product.getImage().getBinaryStream());
		} else {
			throw new NoSuchElementException("No image");
		}
	}

	public void savePostImage(long id, MultipartFile imageFile) throws IOException {
		Product post = repository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Not found"));

		byte[] imageBytes = imageFile.getBytes();
		try {
			Blob imageBlob = new SerialBlob(imageBytes); 
			post.setImage(imageBlob);
			repository.save(post); 
		} catch (SQLException e) {
			throw new IOException("Error", e);
		}
	}

	public ProductDTO updateProduct(long id, ProductDTO pdto, MultipartFile imageFile) throws IOException {
		Product product = repository.getReferenceById(id);
		product.setName(pdto.getName());
		product.setDescription(pdto.getDescription());
		product.setIniValue(pdto.getIniValue());
	
		if (imageFile != null && !imageFile.isEmpty()) {
			this.savePostImage(id, imageFile);
		}
	
		this.save(product);
		return ProductMapper.INSTANCE.toDTO(product);
	}

	public ProductDTO createProduct(ProductDTO productDTO, PublicUserDTO user){
		Date iniHour = new Date(System.currentTimeMillis());
		Date endHour = new Date(iniHour.getTime() + (Long) productDTO.getDuration() * 24 * 60 * 60 * 1000);	
		Product product = new Product(productDTO.getName(),
		productDTO.getDescription(), 
		productDTO.getIniValue(), 
		iniHour, endHour, 
		"In progress", null, userModelRepository.getReferenceById(user.getId()));
		product.setDuration(productDTO.getDuration());
		this.save(product);

		return ProductMapper.INSTANCE.toDTO(product);
	}

	public Page<ProductBasicDTO> findProducts(Pageable pageable) {
    return repository.findAll(pageable).map(this::toDTO);   
	}


	public Page<ProductBasicDTO> findProductsByUser(Pageable pageable, Long id) {
		return repository.findBySeller_Id(pageable, id).map(this::toDTO);		
	}

	public Page<ProductBasicDTO> findBoughtProductsByUser(Pageable pageable, Long id) {
		return repository.findBoughtProductsByUserID(pageable, id).map(this::toDTO);
	}

	private ProductBasicDTO toDTO(Product product){
		return basicMapper.toDTO(product);
	}

	public OfferDTO PlaceABid(ProductDTO product,double cost, PublicUserDTO user) {
		//Get last bid
        Offer lastOffer = offerRepository.findLastOfferByProduct(product.getId());
        //Set min cost
        double actualPrice;
        if (lastOffer != null) {
            actualPrice = lastOffer.getCost();
        } else {
            actualPrice = product.getIniValue() - 1;
        }
        if(cost>actualPrice){
            long currentTime = System.currentTimeMillis();
            Date currentDate = new Date(currentTime); 
			Optional<Product> prod = repository.findById(product.getId());
			UserModel us= userMapper.toDomain(user);
            Offer newOffer=new Offer(us,prod.get(),cost,currentDate);

            prod.get().getOffers().add(newOffer); 
            offerRepository.save(newOffer);
            this.save(prod.get());
			return offerMapper.toDTO(newOffer);
		}else{
			return null;
		}
	}

	public void createTransaction(long id_product) {
		Offer offer = offerRepository.findLastOfferByProduct(id_product);
		Optional<Product> product = repository.findById(id_product);
		double cost = offer.getCost();
		UserModel buyer = offer.getUser();
		UserModel seller = product.get().getSeller();
		Transaction transaction = new Transaction(product.get(), seller, buyer, cost);
		transactionRepository.save(transaction);
	}

	public void checkProduct(long id_product) {
		Optional<Product> product=repository.findById(id_product);
		if(product.isPresent()){
			long actualTime = System.currentTimeMillis();
			if (product.get().getEndHour().getTime() <= actualTime && product.get().getState().equals("In proccess")) {
				product.get().setState("Finished");

				if (!product.get().getOffers().isEmpty()) {
					this.createTransaction(id_product);
				}
			}
			this.save(product.get());
		}
	}

	public double[] getOffersToGrafic(long id_product, double[] costs) {
		Optional<Product> product=repository.findById(id_product);
		if(product.isPresent()){
			List<Offer> offers = product.get().getOffers();
			int numOffers = offers.size();
			if (numOffers > 0) {
				costs = new double[numOffers];
				for (int i = 0; i < numOffers; i++) {
					costs[i] = offers.get(i).getCost();
				}
			} else {
				costs = new double[0];
			}
		}
		return costs;
	}

    public Optional<ProductDTO> setStateDeliveredProduct(long id_product) {
        Optional<Product> product= repository.findById(id_product);
		if(product.isPresent()){
			Product prod = product.get();
			prod.setState("Delivered");
            repository.save(prod);
			return Optional.of(mapper.toDTO(prod));
		}
		return Optional.empty();
		
    }

	public void setStateFinishedProduct(long id_product) {
		Optional<Product> product= repository.findById(id_product);

		if(product.isPresent()){
			product.get().setState("Finished");
            repository.save(product.get());
		}
	}
}
