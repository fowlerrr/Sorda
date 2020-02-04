import { Component, OnInit } from '@angular/core';
import { LoginServiceService } from '../login-service.service';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  public listedItems: Array<any>
  public myItems: Array<any>

  public node: string = ''

  public tokenAmount: number = 10

  public newItemName: string
  public newItemPrice: number = 1.0
  public newItemExpiry: string = new Date().toISOString()

  constructor(private login: LoginServiceService, private http: HttpClient) { }

  ngOnInit() {
    setInterval(this.fetchItems.bind(this), 2000)

    if(this.login.data) {
      this.node = this.login.data.name
    }
  }

  fetchItems() {
    if (this.login.data) {
      const self = this
      this.http.get(this.login.data.url + 'items').subscribe((data) => {
        const oldList = self.listedItems
        const newList = []

        const set = new Set<string>();
        (data as Array<any>).forEach((i) => {
          if (!set.has(i.bidId)) {
            if (oldList) {
              const existing = oldList.find((o) => o.itemId === i.itemId)
              if (existing) {
                i.bidAmount = existing.bidAmount
              }
            }

            if (!i.bidAmount) {
              i.bidAmount = i.lastPrice + 1
            }

            if (i.lastBidder.indexOf(this.login.data.name) > 0) {
              console.log("DSDFDSFSDF")
              i.winning = true
            } else {
              i.winning = false
            }

            set.add(i.bidId)
            newList.push(i)
          }
        })

        self.listedItems = newList

        self.http.get(this.login.data.url + 'items2').subscribe((data) => {
          self.myItems = (data as Array<any>).filter((i) => {
            if (i.owner.indexOf(this.login.data.name) > 0) {
              return true
            }
            return false
          })

          self.myItems.forEach((my) => {
            const match = newList.find((i) => i.itemId === my.id)
            if (match) {
              my.listed = true
            } else {
              my.listed = false
            }
          })
        });
      })
    }
  }

  // fetchCurrentItems() {
  //   if (this.login.data) {
  //     const self = this
  //     this.http.get(this.login.data.url + 'items').subscribe((data) => {
  //       self.listedItems = data
  //     })
  //   }
  // }

  // fetchMyItems() {
  //   if(this.login.data) {
  //     const self = this
  //     this.http.get(this.login.data.url + 'items2').subscribe((data) => {
  //       self.myItems = (data as Array<any>).filter((i) => {
  //         if(i.owner.indexOf(this.login.data.name) > 0) {
  //           return true
  //         }
  //         return false
  //       })
  //     });
  //   }
  // }

  bid(item) {
    this.http.post(this.login.data.url + 'bid', {
      itemId: item.itemId,
      bidId: item.bidId,
      amount: item.bidAmount
    }).subscribe()
  }

  addTokens() {
    this.http.post(this.login.data.url + 'tokens', {
      amount: this.tokenAmount
    }).subscribe()
  }

  createItem() {
    // console.log(this.login.data)
    this.http.post(this.login.data.url + 'list_new_item', {
      description: this.newItemName,
      amount: this.newItemPrice,
      expiry: this.newItemExpiry
    }).subscribe()
  }

}
