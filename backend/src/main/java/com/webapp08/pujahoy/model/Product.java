package com.webapp08.pujahoy.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.sql.Blob;
import java.util.ArrayList;
import java.sql.Date;
import java.util.List;

@Entity
public class Product {
        
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private long id;
    
        private String name;
        private String description;
        private Double iniValue;
        private Date iniHour;
        private Date endHour;
        private String state;
        private Long duration;
        
        @Lob
        private Blob image;
        private String imgURL;

        @OneToMany(mappedBy="product")
        private List<Offer> offers;

        @ManyToOne
        private UserModel seller;

        @OneToOne(mappedBy = "product", cascade = CascadeType.ALL) 
        private Rating rating;
    
        protected Product(){
    
        }

        public Product(String name,String description,Double iniValue, Date iniHour,Date endHour, String state, Blob image, UserModel seller){
            this.name = name;
            this.description = description;
            this.iniValue = iniValue;
            this.iniHour = iniHour;
            this.endHour = endHour;
            this.state = state;
            this.image = image;
            this.seller = seller;
            this.offers = new ArrayList<Offer>();
        }

        

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        
        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

       
        public Double getIniValue() {
            return iniValue;
        }

        public void setIniValue(Double iniValue) {
            this.iniValue = iniValue;
        }

        public Date getIniHour() {
            return iniHour;
        }

        public void setIniHour(Date iniHour) {
            this.iniHour = iniHour;
        }

        public Date getEndHour() {
            return endHour;
        }

        public void setEndHour(Date endHour) {
            this.endHour = endHour;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public Blob getImage() {
            return image;
        }

        public void setImage(Blob image) {
            this.image = image;
        }

        public UserModel getSeller() {
            return seller;
        }

        public void setSeller(UserModel seller) {
            this.seller = seller;
        }

        public List<Offer> getOffers() {
            return offers;
        }

        public void setOffers(List<Offer> offers) {
            this.offers = offers;
        }

        public void addOffer(Offer offer){
            this.offers.add(offer);
        }

        public String getImgURL() {
            return imgURL;
        }

        public void setImgURL(String imgURL) {
            this.imgURL = imgURL;
        }

        public Long getDuration() {
            return duration;
        }

        public void setDuration(Long duration) {
            this.duration = duration;
        } 

        public boolean isActive() {
        long currentTime = System.currentTimeMillis();
        Date currentDate = new Date(currentTime); 

        return iniHour != null && endHour != null && !currentDate.before(iniHour) && !currentDate.after(endHour);
        }

}
