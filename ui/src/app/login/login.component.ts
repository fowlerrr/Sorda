import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { LoginServiceService } from '../login-service.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  private nodes = {
    'NodeA': {
      url: 'http://localhost:8080/',
      name: 'NodeA'
    },
    'NodeB': {
      url: 'http://localhost:8083/',
      name: 'NodeB'
    },
    'NodeC': {
      url: 'http://localhost:8084/',
      name: 'NodeC'
    }
  }
  selectedNode: any = 'NodeA'

  constructor(private router: Router, private service: LoginServiceService) { }

  ngOnInit() {
  }

  selectChangeHandler(event: any) {
    this.selectedNode = event.target.value;
  }

  login() {
    this.service.data = this.nodes[this.selectedNode]
    this.router.navigate(['dashboard'])
  }
}
