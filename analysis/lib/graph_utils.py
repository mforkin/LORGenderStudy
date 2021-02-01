import plotly.graph_objects as go


def show_bar_plot (x_vals, y_vals, title='Bar Chart'):
    fig = go.Figure(
        data=[go.Bar(x=x_vals, y=y_vals)],
        layout=go.Layout(
            title=go.layout.Title(text=title)
        )
    )
    fig.show()
