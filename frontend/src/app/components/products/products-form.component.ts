import { Component, OnInit } from '@angular/core';
import { productsService } from '../../services/products.service';
import { Router } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { LoginService } from '../../services/login.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-products-form',  
  templateUrl: './products-form.component.html',
  styleUrls: ['./products-form.component.css']
})

export class ProductsFormComponent implements OnInit {
  errorMessage: string | undefined;
  selectedFileName: string = '';
  isEditMode: boolean = false
  productId: number | undefined
  isAllowed: boolean = true;
  isLoading: boolean = true;
  constructor(private productsService: productsService, private router: Router, private route: ActivatedRoute, private loginService:LoginService) { }

  ngOnInit(): void {
    this.selectedFileName = '';
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.productId = +id;
        this.loadProduct(this.productId);
      }
    });
    this.isLoading = false;
  }

  product = {
    name: '',
    description: '',
    iniValue: 0,
    duration: 0,
    hasBids: false
  };

  image: File | null = null;

  submitForm() { //submits the form regardless of whether or not it is the edit or new auction form
    
    if (!this.isValid()){
      return
    }

    const productToSend = {
      name: this.product.name,
      description: this.product.description,
      iniValue: this.product.iniValue,
      duration: this.product.duration
    };

    if (this.isEditMode){
      this.productsService.editProduct(productToSend, this.productId).subscribe(
        (createdProduct: any) => {
          this.errorMessage = '';
    
          if (this.selectedFileName != "" && this.image) {
            const formData = new FormData();
            formData.append('image', this.image);
    
            this.productsService.putImage(createdProduct.id, formData).subscribe(
              () => {
                this.router.navigate(['/product/'+this.productId]);
              },
              (error) => {
                window.alert('Could not upload the image: ' + error.message);
              }
            );
          } else {
            this.router.navigate(['/product/'+this.productId]);
          }
        },
        (error) => {
          window.alert('If another user placed a bid, you cannot edit the auction');
          this.router.navigate(['/product/'+this.productId]);
        }
      );
    } else {
      this.productsService.createProduct(productToSend).subscribe(
        (createdProduct: any) => {
          this.errorMessage = '';
    
          if (this.image) {
            const formData = new FormData();
            formData.append('image', this.image);
    
            this.productsService.uploadImage(createdProduct.id, formData).subscribe(
              () => {
                this.router.navigate(['/product/'+ createdProduct?.id]);
              },
              (error) => {
                window.alert('Could not upload the image: ' + error.message);
              }
            );
          } else {
            this.router.navigate(['/product/'+ createdProduct?.id]);
          }
        },
        (error) => {
          window.alert('Could not create product: ' + error.message);
        }
      );
    }
  
    
  }

  isValid(): boolean { // checks if the form is completely filled in and if not it stops the user from submitting
    const validImageTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/jpg'];
  
    if (!this.product.name || this.product.name.trim() === '') {
      alert('Please provide a name.');
      return false;
    }
  
    if (!this.product.description || this.product.description.trim() === '') {
      alert('Please provide a description.');
      return false;
    }
  
    if (this.product.iniValue <= 0) {
      alert('Initial value must be greater than 0.');
      return false;
    }
  
    if (!this.isEditMode){
      if (this.product.duration <= 0) {
        alert('Must Select a duration');
        return false;
      }
      if (!this.image) {
        alert('Please upload an image before submitting.');
        return false;
      }
      if (!validImageTypes.includes(this.image.type)) {
        alert('Please select a valid image file (JPEG, JPG, PNG, WEBP).');
        return false;
      }
    }
  
    return true;
  }
  
  
  onImageSelected(event: any) { // Shows the name of the image provided by the user
    const file = event.target.files[0];
    this.image = file;
    this.selectedFileName = file.name;
    console.log('Image selected:', file);
  }

  goBack() { //redirects user to index
    this.router.navigate(['/']);
  }


  loadProduct(id: number) { // loads the product if the user is in edit mode
    forkJoin({
      product: this.productsService.getProductById(id),
      user: this.loginService.reqUser() 
    }).subscribe({
      next: ({ product, user: user }) => {
        if (user.rols.indexOf("ADMIN") === -1 && product.seller.id !== user.id) {
          this.isAllowed=false
          this.isLoading = false
          return;
        }  
        this.product = {
          name: product.name,
          description: product.description,
          iniValue: product.iniValue,
          duration: product.duration,
          hasBids: product.offers.length>0
        };

        if (this.product.hasBids){
          this.isAllowed=false
        }

        this.isLoading = false
        console.log(product.duration);
      },
      error: (err) => {
        this.isAllowed=false
        this.isLoading = false
      }
    });
  }

}